# Spring Shopping Cart MCP Server

A Java 21 and Spring Boot server that exposes shopping-cart and user-management operations as [Model Context Protocol (MCP)](https://modelcontextprotocol.io/) tools. It uses Spring AI for MCP integration, Spring Data JPA for persistence, and Microsoft SQL Server as its database.

## Features

- Add products to a cart, remove products, list cart items, and calculate the cart total.
- Create and update users; search users by name or location; list all users.
- Runs as an MCP server over standard input/output (stdio), suitable for local MCP clients.
- Uses SQL Server-backed JPA entities and repositories.

## Technology stack

- Java 21
- Spring Boot 3.5.5
- Spring AI 1.0.1 MCP Server
- Spring Data JPA / Hibernate
- Microsoft SQL Server JDBC driver
- Maven

## Prerequisites

- JDK 21 or later
- Maven 3.9 or later
- Microsoft SQL Server reachable from the machine running the server
- An existing database, such as `DIVAHOTEL_DEV`, and a SQL login with the required permissions
- Claude Code Desktop, Claude Desktop with a supported MCP configuration, or [MCP Inspector](https://github.com/modelcontextprotocol/inspector) for testing

## Database configuration

Set the SQL login in the environment before launching the server. Do not commit real credentials to the repository.

PowerShell example for the current terminal:

```powershell
$env:DB_USERNAME = "your-sql-login"
$env:DB_PASSWORD = "your-sql-password"
```

Configure the database URL in `src/main/resources/application.properties` if your SQL Server host, port, or database differs from the defaults. The application expects these environment variables:

- `DB_USERNAME` — SQL Server login
- `DB_PASSWORD` — SQL Server password

The JDBC URL currently targets `localhost:1433` and database `DIVAHOTEL_DEV`, with encryption enabled and certificate validation trusted for local development. Use appropriate certificate validation and secrets management outside local development.

The JPA setting `spring.jpa.hibernate.ddl-auto=update` updates mapped tables, but does not create the SQL Server database itself. The mapped tables are `cart_items` and `demouser`. The `demouser` entity maps the `id`, `name`, `location`, `created_by`, `created_date`, and optimistic-lock `version` fields. If using a pre-existing schema, ensure its columns and constraints are compatible with these mappings.

## Build

From the repository root:

```powershell
mvn clean package -DskipTests
```

The executable MCP server JAR is created at `target/shopping-cart-server.jar`. Stop any running MCP Inspector or server process before rebuilding on Windows if the JAR is locked.

To run the automated tests:

```powershell
mvn test
```

The Spring context test connects to the configured SQL Server, so database access and credentials must be available when running the tests.

## Connect with Claude Code Desktop

This repository includes a project-scoped `.mcp.json` configured to launch `target/shopping-cart-server.jar` with stdio.

1. Set `DB_USERNAME` and `DB_PASSWORD` in your Windows user environment, then fully restart Claude Code Desktop so it inherits the variables.
2. Build the JAR using the command above.
3. Open this repository folder in the **Code** tab using a **Local** session.
4. Approve the project MCP server if prompted.
5. Check the server status with `/mcp` in the Code session.
6. Ask Claude to use a tool, for example: “Use `getAllUsers` to list all users.”

The MCP server is local to the selected project. A regular Claude Chat conversation does not load this project-scoped `.mcp.json` configuration.

## Deploy the REST API to Amazon EC2

The `ec2` Spring profile is provided in `src/main/resources/application-ec2.properties`. The application exposes the user REST API on port `8080` when running in servlet mode. The current MCP transport remains stdio; it is for a local MCP client that launches the JAR, not a remote MCP connection to EC2.

### 1. Build the deployable JAR locally

On a machine with JDK 21 and Maven installed, from the repository root run:

```powershell
mvn clean package -DskipTests
```

This produces `target/shopping-cart-server.jar`. Stop any process using the JAR before rebuilding on Windows.

### 2. Transfer the JAR to EC2

The following assumes an Amazon Linux 2023 EC2 instance and SSH access using your own key. Run from PowerShell, replacing `<EC2_PUBLIC_IP>` with your instance address and using your private-key path:

```powershell
scp -i "C:\path\to\your-key.pem" target/shopping-cart-server.jar ec2-user@<EC2_PUBLIC_IP>:/tmp/shopping-cart-server.jar
```

Connect to the EC2 instance, install Java 21, and move the JAR into its application directory:

```bash
sudo dnf install -y java-21-amazon-corretto-headless
java -version
sudo mkdir -p /opt/shopping-cart-mcp
sudo install -o ec2-user -g ec2-user -m 0644 /tmp/shopping-cart-server.jar /opt/shopping-cart-mcp/shopping-cart-server.jar
rm /tmp/shopping-cart-server.jar
```

Keep EC2 in the same VPC as RDS or ensure the VPCs are routed. The RDS security group should allow inbound TCP `1433` from the EC2 instance's security group.

### 3. Configure the EC2 service

Create `/etc/shopping-cart-mcp.env` on EC2 with the SQL credentials. Do not put credentials in the JAR, repository, shell command history, or screenshots. Prefer AWS Secrets Manager or Systems Manager Parameter Store for production secrets. Use a dedicated least-privilege application login in production rather than the RDS master login.

```ini
DB_USERNAME=your-application-login
DB_PASSWORD=replace-with-the-secret
```

Restrict the credentials file and allow the service account to read it:

```bash
sudo chown root:ec2-user /etc/shopping-cart-mcp.env
sudo chmod 640 /etc/shopping-cart-mcp.env
```

Install the repository's `deploy/shopping-cart-mcp.service` at `/etc/systemd/system/shopping-cart-mcp.service`. Its contents are:

### 3. Configure the EC2 service

Create `/etc/systemd/system/shopping-cart-mcp.service` with:

```ini
[Unit]
Description=Shopping Cart Spring MCP and REST API
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/shopping-cart-mcp
EnvironmentFile=/etc/shopping-cart-mcp.env
ExecStart=/usr/bin/java -jar /opt/shopping-cart-mcp/shopping-cart-server.jar --spring.profiles.active=ec2
Restart=on-failure
RestartSec=5
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

Ensure the JAR and log directory are accessible to `ec2-user`. Then reload systemd and start the service:

```bash
sudo systemctl daemon-reload
sudo systemctl enable --now shopping-cart-mcp
sudo systemctl status shopping-cart-mcp
```

View application logs with:

```bash
sudo tail -f /home/ec2-user/shopping-cart-mcp.log
```

The service connects to RDS and Hibernate updates the mapped tables in `DIVAHOTEL_DEV`; the database itself must already exist. Configure the RDS security group to allow inbound TCP `1433` from the EC2 instance's security group.

### 4. Test the REST API safely

The user REST endpoints are:

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/users` | Create a user. JSON body: `{"name":"Alex","location":"Seattle"}` |
| `PUT` | `/api/users/{id}` | Update a user's name and location with the same JSON body shape. |
| `GET` | `/api/users` | List all users. |
| `GET` | `/api/users/search?name=Alex` | Search user names, case-insensitively. |

On EC2, test with `curl http://localhost:8080/api/users`. From Windows, use an SSH tunnel rather than opening port `8080` publicly:

```powershell
ssh -i "C:\path\to\your-key.pem" -L 8080:localhost:8080 ec2-user@<EC2_PUBLIC_IP>
```

Keep the tunnel open and use `http://localhost:8080/api/users` from your local API client. For production external access, add authentication/authorization and HTTPS (for example, through a secured load balancer). The current REST controller has no authentication; do not expose port `8080` publicly. The current stdio MCP configuration does not become a remote MCP endpoint just by deploying the JAR to EC2.

## Test with MCP Inspector

With the required database environment variables set and the JAR built, run:

```powershell
npx -y @modelcontextprotocol/inspector java -jar "D:\Projects\spring-mcp\target\shopping-cart-server.jar"
```

Open the Inspector URL printed in the terminal, connect to the `java` stdio server, select **Tools**, choose a tool, provide its arguments, and run it. If Inspector's default web port is occupied, set `CLIENT_PORT` to a free port before launching Inspector.

## Available MCP tools

### User tools

| Tool | Arguments | Description |
| --- | --- | --- |
| `addUser` | `name`, `location` | Create a user and return the saved record, including its ID. |
| `updateUser` | `id`, `name`, `location` | Update the user's name and location by ID. Returns `null` when the ID is not found. |
| `getUsersByName` | `name` | Find users whose names contain the given text, ignoring case. |
| `getAllUsers` | None | Return all users. |
| `getUsersByLocation` | `location` | Find users whose locations contain the given text, ignoring case. |

### Shopping-cart tools

| Tool | Arguments | Description |
| --- | --- | --- |
| `addToCart` | `productName`, `quantity` | Add a catalog product or increase its existing cart quantity. |
| `removeCart` | `productName` | Remove that product from the cart. |
| `getCarts` | None | Return all cart items. |
| `getCartTotal` | None | Return the sum of cart item prices. |

The available product names are `iPhone`, `MacBook Air`, and `Boat Airdopes`. Names must match the catalog spelling and capitalization.

## Example prompts

- “Add a user named Alex in Seattle.”
- “Show all users.”
- “Find users whose name contains `Alex`.”
- “Find users located in `Seattle`.”
- “Add two iPhones to the cart.”
- “Show my cart total.”

## Project layout

```text
src/main/java/com/divatech/
├── ShoppingCartMcpApplication.java
├── entity/         # JPA entities
├── respository/    # Spring Data repositories
└── tools/          # MCP tool services
src/main/resources/
└── application.properties
src/test/java/com/divatech/
```

## Troubleshooting

- **MCP server fails to start:** confirm the JAR exists, `java -version` works, SQL Server is reachable, and `DB_USERNAME` / `DB_PASSWORD` are available to the process.
- **Claude Code does not list the tools:** use a Local Code session opened at this repository, approve the project server, and restart the session after rebuilding.
- **Inspector says the port is already in use:** choose a free port, for example `$env:CLIENT_PORT = "6384"` in PowerShell, before starting Inspector.
- **SQL insert reports a missing non-null column:** confirm the target table schema matches the corresponding JPA entity, including required audit and version columns.
- **Maven cannot delete the JAR:** disconnect the MCP server and stop the Java process using `target/shopping-cart-server.jar`, then rebuild.

## Security

Never commit database passwords, API keys, or other secrets. Use environment variables or a local secrets manager. If a real credential was previously committed or published, rotate it in SQL Server and update the local environment.
