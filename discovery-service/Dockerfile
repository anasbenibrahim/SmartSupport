# Étape 1 : Construction du projet avec Maven et Java 17
FROM maven:3.8.7-eclipse-temurin-17 AS build
WORKDIR /app

# Copie des fichiers de configuration Maven et du code source
COPY pom.xml ./
COPY .mvn .mvn
COPY src src

# Compilation du projet et création du JAR en ignorant les tests
RUN mvn clean package -DskipTests

# Étape 2 : Exécution avec une image JRE légère
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copie du fichier JAR généré depuis l'étape précédente
COPY --from=build /app/target/*.jar app.jar

# Commande de lancement de l'application
ENTRYPOINT ["java", "-jar", "app.jar"]