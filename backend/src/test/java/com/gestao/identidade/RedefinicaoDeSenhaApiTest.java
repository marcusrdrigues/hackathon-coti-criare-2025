package com.gestao.identidade;

import com.gestao.compartilhado.aplicacao.EnvioDeEmail;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * "Esqueci minha senha" (spec 004). Sem {@code @Transactional} de propósito: o e-mail só sai
 * depois de o link estar gravado, então cada pedido precisa confirmar a sua transação. Os
 * e-mails ficam numa caixa de saída de teste, e nenhum sai de verdade.
 */
@SpringBootTest
@ExtendWith(OutputCaptureExtension.class)
class RedefinicaoDeSenhaApiTest {

    private static final String SENHA = "segredo123";
    private static final String SENHA_NOVA = "outraSenha456";
    private static final String LINK_INVALIDO = "Este link não vale mais. Peça um novo na tela de login.";
    private static final Pattern TOKEN_NO_LINK = Pattern.compile("/redefinir-senha#token=([A-Za-z0-9_-]+)");

    @Autowired private WebApplicationContext contexto;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private CaixaDeSaida caixa;

    @Value("${app.superadmin.email}") private String emailDoSuperadmin;

    private MockMvc mvc;
    private String dominio;
    private String email;

    /** Troca o provedor de e-mail por uma caixa de saída que os testes leem. */
    @TestConfiguration
    static class ComCaixaDeSaida {

        @Bean
        @Primary
        CaixaDeSaida caixaDeSaida() {
            return new CaixaDeSaida();
        }
    }

    static class CaixaDeSaida implements EnvioDeEmail {

        private final ConcurrentLinkedQueue<Email> enviados = new ConcurrentLinkedQueue<>();

        @Override
        public void enviar(Email email) {
            if (email.para().startsWith("falha@")) {
                throw new FalhaNoEnvioDeEmail("Provedor fora do ar (simulado).", null);
            }
            enviados.add(email);
        }

        /** O envio é assíncrono: espera até 5 segundos pelo e-mail desta pessoa. */
        List<Email> esperarPor(String para, int quantidade) throws InterruptedException {
            long limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
            while (System.nanoTime() < limite) {
                List<Email> daPessoa = para(para);
                if (daPessoa.size() >= quantidade) {
                    return daPessoa;
                }
                Thread.sleep(50);
            }
            return para(para);
        }

        List<Email> para(String para) {
            return enviados.stream().filter(e -> e.para().equals(para)).toList();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(contexto).apply(springSecurity()).build();
        dominio = "redefinicao-" + UUID.randomUUID().toString().substring(0, 8) + ".com";
        email = "ana@" + dominio;
        cadastrar(email, "Ana Ribeiro");
    }

    @Test
    void pedidoComContaEnviaOLinkEOBancoGuardaSoOHash() throws Exception {
        pedir(email);

        List<EnvioDeEmail.Email> enviados = caixa.esperarPor(email, 1);
        assertThat(enviados).singleElement().satisfies(enviado -> {
            assertThat(enviado.assunto()).isEqualTo("Redefinição de senha do Portal Criare");
            assertThat(enviado.texto()).contains("Olá, Ana,").contains("30 minutos").contains("ignore este e-mail");
            assertThat(enviado.html()).contains("Criar uma senha nova");
        });
        String token = token(enviados.getFirst());

        List<String> hashes = jdbc.queryForList(
                "SELECT token_hash FROM tb_redefinicao_senha WHERE usuario_id = ?", String.class, idDaPessoa(email));
        assertThat(hashes).containsExactly(sha256(token)).doesNotContain(token);
    }

    @Test
    void respostaEhAMesmaComContaOuSem() throws Exception {
        MvcResult comConta = pedir(email);
        MvcResult semConta = pedir("ninguem@" + dominio);

        assertThat(semConta.getResponse().getStatus()).isEqualTo(comConta.getResponse().getStatus()).isEqualTo(202);
        assertThat(semConta.getResponse().getContentAsString()).isEqualTo(comConta.getResponse().getContentAsString())
                .isEmpty();
        assertThat(caixa.esperarPor(email, 1)).hasSize(1);
        assertThat(caixa.para("ninguem@" + dominio)).isEmpty();
    }

    @Test
    void contaDeExemploESuperadminNaoRecebemOLink() throws Exception {
        String exemplo = "exemplo-" + UUID.randomUUID().toString().substring(0, 8) + "@demo.com";
        cadastrar(exemplo, "Conta de Exemplo");

        pedir(exemplo);
        pedir(emailDoSuperadmin);

        assertThat(links(exemplo)).isZero();
        assertThat(links(emailDoSuperadmin)).isZero();
        assertThat(caixa.para(exemplo)).isEmpty();
    }

    @Test
    void passandoDeTresPedidosPorHoraNaoEnviaMais() throws Exception {
        for (int i = 0; i < 4; i++) {
            pedir(email);
        }

        assertThat(links(email)).isEqualTo(3);
        assertThat(caixa.esperarPor(email, 3)).hasSize(3);
    }

    @Test
    void falhaNoProvedorNaoMudaAResposta() throws Exception {
        String comFalha = "falha@" + dominio;
        cadastrar(comFalha, "Pessoa Sem Sorte");

        pedir(comFalha);

        assertThat(links(comFalha)).isEqualTo(1);
    }

    @Test
    void linkTrocaASenhaUmaVezSoEEncerraAsSessoes(CapturedOutput saida) throws Exception {
        Cookie sessaoAntiga = cookie(login(email, SENHA, 200));
        String token = pedirEReceber();

        consultar(token, 204);
        confirmar(token, SENHA_NOVA, 204);

        // A sessão aberta com a senha antiga caiu, a senha antiga não entra mais e a nova entra
        mvc.perform(post("/api/v1/auth/refresh").cookie(sessaoAntiga)).andExpect(status().isUnauthorized());
        login(email, SENHA, 401);
        login(email, SENHA_NOVA, 200);

        // O link não serve uma segunda vez
        confirmar(token, "maisUmaSenha789", 404).andExpect(jsonPath("$.detail").value(LINK_INVALIDO));
        consultar(token, 404);

        // Nem o token nem a senha aparecem no log
        assertThat(saida.getAll()).doesNotContain(token).doesNotContain(SENHA_NOVA).doesNotContain(email);
    }

    @Test
    void pedidoNovoInvalidaOLinkAnterior() throws Exception {
        pedir(email);
        String primeiro = token(caixa.esperarPor(email, 1).getFirst());
        pedir(email);
        String segundo = token(caixa.esperarPor(email, 2).get(1));

        consultar(primeiro, 404).andExpect(jsonPath("$.detail").value(LINK_INVALIDO));
        consultar(segundo, 204);
    }

    @Test
    void linkVencidoOuInventadoNaoVale() throws Exception {
        String token = pedirEReceber();
        jdbc.update("UPDATE tb_redefinicao_senha SET expira_em = ? WHERE token_hash = ?",
                OffsetDateTime.now().minusMinutes(1), sha256(token));

        consultar(token, 404).andExpect(jsonPath("$.detail").value(LINK_INVALIDO));
        confirmar(token, SENHA_NOVA, 404);
        consultar("inventado", 404).andExpect(jsonPath("$.detail").value(LINK_INVALIDO));
    }

    @Test
    void senhaForaDasRegrasNaoGastaOLink() throws Exception {
        String token = pedirEReceber();

        confirmar(token, "curta", 400).andExpect(jsonPath("$.erros.senha").exists());
        confirmar(token, "semnumeros", 400);

        consultar(token, 204);
        confirmar(token, SENHA_NOVA, 204);
    }

    @Test
    void trocaZeraOBloqueioDeLogin() throws Exception {
        for (int i = 0; i < 5; i++) {
            login(email, "senhaErrada" + i, 401);
        }
        login(email, SENHA, 429);

        confirmar(pedirEReceber(), SENHA_NOVA, 204);

        login(email, SENHA_NOVA, 200);
    }

    @Test
    void pedidoETrocaViramEventosDeSeguranca() throws Exception {
        confirmar(pedirEReceber(), SENHA_NOVA, 204);

        List<String> tipos = jdbc.queryForList(
                "SELECT tipo FROM tb_evento_seguranca WHERE usuario_id = ? AND email_mascarado = ? ORDER BY ocorrido_em",
                String.class, idDaPessoa(email), "a***@" + dominio);
        assertThat(tipos).containsSubsequence("REDEFINICAO_DE_SENHA_PEDIDA", "SENHA_REDEFINIDA");
    }

    // ---------------------------------------------------------------- apoio

    private String pedirEReceber() throws Exception {
        int antes = caixa.para(email).size();
        pedir(email);
        return token(caixa.esperarPor(email, antes + 1).getLast());
    }

    private MvcResult pedir(String para) throws Exception {
        return mvc.perform(post("/api/v1/auth/redefinicao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + para + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    private ResultActions consultar(String token, int esperado) throws Exception {
        return mvc.perform(post("/api/v1/auth/redefinicao/consulta").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().is(esperado));
    }

    private ResultActions confirmar(String token, String senha, int esperado)
            throws Exception {
        return mvc.perform(post("/api/v1/auth/redefinicao/confirmacao").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().is(esperado));
    }

    private MvcResult login(String para, String senha, int esperado) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + para + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().is(esperado))
                .andReturn();
    }

    private void cadastrar(String para, String nome) throws Exception {
        mvc.perform(post("/api/v1/cadastro").contentType(MediaType.APPLICATION_JSON).content("""
                        {"tipo":"EMPRESA","razaoSocial":"Organização %s","cnpj":"%s","nome":"%s","email":"%s","senha":"%s"}
                        """.formatted(para, cnpjAleatorio(), nome, para, SENHA)))
                .andExpect(status().isCreated());
    }

    private long links(String para) {
        return Optional.ofNullable(jdbc.queryForObject("SELECT COUNT(*) FROM tb_redefinicao_senha r "
                + "JOIN tb_usuario u ON u.id = r.usuario_id WHERE u.email = ?", Long.class, para)).orElse(0L);
    }

    private UUID idDaPessoa(String para) {
        return jdbc.queryForObject("SELECT id FROM tb_usuario WHERE email = ?", UUID.class, para);
    }

    private static String token(EnvioDeEmail.Email enviado) {
        Matcher link = TOKEN_NO_LINK.matcher(enviado.texto());
        assertThat(link.find()).as("o e-mail traz o link com o token").isTrue();
        return link.group(1);
    }

    private static Cookie cookie(MvcResult resultado) {
        String setCookie = resultado.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        return new Cookie("refresh_token", setCookie.substring("refresh_token=".length(), setCookie.indexOf(';')));
    }

    private static String sha256(String texto) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }

    /** Um CNPJ com dígitos verificadores válidos, para os dados deste teste não colidirem com outros. */
    private static String cnpjAleatorio() {
        int[] numeros = new int[14];
        for (int i = 0; i < 8; i++) {
            numeros[i] = ThreadLocalRandom.current().nextInt(10);
        }
        numeros[11] = 1;
        numeros[12] = digito(numeros, 12);
        numeros[13] = digito(numeros, 13);
        StringBuilder cnpj = new StringBuilder();
        for (int n : numeros) {
            cnpj.append(n);
        }
        return cnpj.toString();
    }

    private static int digito(int[] numeros, int tamanho) {
        int[] pesos = tamanho == 12
                ? new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2}
                : new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int soma = 0;
        for (int i = 0; i < tamanho; i++) {
            soma += numeros[i] * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
