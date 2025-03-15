# ParkNPlug


## Overview
This is a hobby project that I plan to build iteratively while learning in the process. The server is built using vanilla Java and uses SQLite as the database. User passwords are hashed for security. Currently, users can register, post data, and retrieve data. As I upgrade the server in the future, I aim to enhance its functionality and transform it into a production-level project.

The server communicates in JSON format, supports multithreading and is designed to be simple yet functional. While I am currently using vanilla Java, my future plan is to migrate to Spring Boot for a more robust backend and integrate React in the frontend to make it a full-stack project. Right now, I am experimenting and exploring ideas, and the project will evolve as I gain new insights and inspiration. The ultimate goal is to build a production-level full-stack application.

## Features
- **User Registration**: Users can register via the `/registration` endpoint.
- **Data Handling**: Registered users can post and retrieve data via the `/datarecord` endpoint.
- **JSON Communication**: The server communicates using JSON format.
- **Multithreading**: The server supports multithreading for handling multiple requests simultaneously.
- **Password Hashing**: User passwords are hashed for security.

## Prerequisites
Before running the server, ensure you have the following installed:
- **Java Development Kit (JDK)**: Version 8 or higher.
- **Keytool**: A command-line utility included with the JDK for managing keystores.
- **Curl**: For testing the server endpoints.
- **Maven**: For building the project.

## Setup Instructions

### 1. Clone the Repository
Clone the project repository to your local machine:

```bash
git clone https://github.com/moiz-naeem/ParkNPlug.git
```
### 2. Build the Project
Navigate to the project directory and build the project using Maven:
```bash
 cd ParkNPlug
mvn clean install
``` 
### 3. Generate Keystore
Navigate to the src/main/java/server directory and generate a keystore file using keytool:

```bash
 cd src/main/java/server
keytool -genkeypair -alias myserver -keyalg RSA -keysize 2048 -validity 365 -keystore keystore.jks
```
- **alias myserver**: Specifies an alias for the key pair (you can change myserver to any name).
- **keyalg RSA**: Specifies the key algorithm (RSA is recommended).
- **keysize 2048**: Specifies the key size (2048 bits is secure).
- **validity 365**: Specifies the validity period of the certificate in days (365 days = 1 year).
- **keystore keystore.jks**: Specifies the name of the keystore file (keystore.jks)

You will be prompted to enter the following details:

- **Keystore password**: Enter a strong password (remember this password as it will be required to run the server).
- **Name, organizational unit, etc.**: Provide relevant details (these are optional but required for the certificate).

### 4. Configure and Run the Server
1. Open the project in your preferred IDE (IntelliJ is recommended).
2. Open the Server.java file.
3. Go to Run Configurations and create a new configuration.

**Main Class**: server.Server .

**CLI Arguments**: Provide the keystore file name (e.g., keystore.jks) and the keystore password you set earlier.
Save the configuration and run the server.

## Testing the Server

#### 1. User Registration
   To register a user, use the /registration endpoint. An example payload is provided in the user.json file. Use the following curl command:
   ```bash
curl -k -d "@user.json" https://localhost:8001/registration -H "Content-Type: application/json"

```

#### 2. Posting and Retrieving Data
   To post or retrieve data, use the /datarecord endpoint. Only registered users can access this endpoint. An example payload is provided in the payload.json file. Use the following curl commands:

**Post Data**:
```bash
  curl -k -d "@payload.json" https://localhost:8001/datarecord -H "Content-Type: application/json" -u Anna:passwords
```

**Get Data:**
```bash
curl -k https://localhost:8001/datarecord -H "Content-Type: application/json" -u Anna:passwords

```

## Future Plans

- Migrate to Spring Boot for a more robust and scalable backend.
- Integrate React for the frontend to create a full-stack application.
- Enhance functionality and add new features iteratively.
- Improve security and performance.
- Explore new ideas and transform the project into a production-level application.

