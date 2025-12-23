# Etapa 1: BUILD (Compilação)
# Instalando uma imagem do JDK para compilar o projeto
FROM eclipse-temurin:21-jdk AS build

#Criando uma pasta para o BUILD do projeto
WORKDIR /app

#Copiar todos os arquivos do projeto
COPY . .

# Adicionar permissão de execução ao mvnw
RUN chmod +x ./mvnw

#Compilar o projeto (BUILD)
RUN ./mvnw clean package -DskipTests

# Etapa 2: DEPLOY (Execução)
#Instalando uma imagem do JRE para executar o projeto
FROM eclipse-temurin:21-jre

#Criando uma pasta para o DEPLOY do projeto
WORKDIR /app

#Copiar os arquivos do BUILD para dentro da pasta /app
COPY --from=build /app/target/Gestao-*.jar app.jar

#Definir a porta de execução
EXPOSE 8085

#Inicializar e rodar a aplicação
ENTRYPOINT ["java", "-jar", "app.jar"]