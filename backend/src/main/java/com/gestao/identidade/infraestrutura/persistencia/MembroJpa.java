package com.gestao.identidade.infraestrutura.persistencia;

import com.gestao.identidade.dominio.Membro;
import com.gestao.identidade.dominio.Papel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data para os vínculos entre pessoas e organizações. */
interface MembroJpa extends JpaRepository<Membro, UUID> {

    @Query("""
            SELECT m FROM Membro m JOIN FETCH m.usuario JOIN FETCH m.organizacao
            WHERE m.usuario.id = :usuarioId AND m.removidoEm IS NULL""")
    Optional<Membro> ativoDoUsuario(@Param("usuarioId") UUID usuarioId);

    @Query("""
            SELECT m FROM Membro m JOIN FETCH m.usuario
            WHERE m.organizacao.id = :organizacaoId AND m.removidoEm IS NULL
            ORDER BY CASE WHEN m.papel = :primeiro THEN 0 ELSE 1 END, m.usuario.nome""")
    List<Membro> ativosDaOrganizacao(@Param("organizacaoId") UUID organizacaoId, @Param("primeiro") Papel primeiro);
}
