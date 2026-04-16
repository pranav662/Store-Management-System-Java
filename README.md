# 🛒 GroceryPro — Store Management System

[![Java](https://img.shields.io/badge/Java-11+-ED8B00?style=for-the-badge&logo=java&logoColor=white)](https://www.java.com)
[![MySQL](https://img.shields.io/badge/MySQL-8.0+-4479A1?style=for-the-badge&logo=mysql&logoColor=white)](https://www.mysql.com)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](https://opensource.org/licenses/MIT)
[![Railway](https://img.shields.io/badge/Railway-Project-blueviolet?style=for-the-badge&logo=railway&logoColor=white)](https://railway.app)

<div align="center">
  <img src="previews/preview_dashboard.png" alt="GroceryPro Dashboard Preview" width="100%">
  <br>
  <h3>Sleek. Fast. Minimalist.</h3>
  <p><em>A high-performance Java web application built from the ground up without heavy frameworks.</em></p>
</div>

---

## 📖 Overview

**GroceryPro** is a robust, production-ready Store Management System that defies modern bloat. It features a **custom-built multi-threaded HTTP server** (using standard Java Sockets), a hand-crafted JSON parser, and a professional glassmorphic dashboard. Whether managing inventory, tracking high-value customers, or generating smart bills, GroceryPro provides a seamless, high-speed experience.

### 🌐 Live Production Environment
Access the hosted version on Railway: [**grocerypro-live.up.railway.app**](https://store-management-system-java-production.up.railway.app)

<p align="center">
  <img src="previews/website_qr.png" alt="Website QR Code" width="180">
  <br>
  <em>Scan to launch the dashboard</em>
</p>

---

## 🏗️ Technical Architecture

GroceryPro implements a unique "No-Framework" architecture, focusing on the core fundamentals of Java Networking and Concurrency.

```mermaid
graph TD
    Client((Browser/User)) -- "HTTP/JSON" --> Server[Custom HttpServer]
    
    subgraph Java Backend (Store.java)
        Server -- "Accept Socket" --> Pool[Thread Handler]
        Pool -- "Parse Request" --> Router{API Router}
        Router -- "Serve Static" --> Static[SPA: index.html]
        Router -- "DAO Logic" --> DAO[JDBC / SQL]
    end
    
    subgraph Data Layer
        DAO -- "TCP/IP" --> MySQL[(MySQL DB)]
    end
    
    subgraph Management
        GUI[Swing Control Panel] -- "Manage" --> Server
    end
```

### 🛠️ Core Technology Stack
- **Backend Core**: Java (OpenJDK 11+)
- **Networking**: Custom Socket-level HTTP/1.1 Server (`ServerSocket`)
- **Database**: MySQL with JDBC (Hosted on Railway)
- **Frontend**: Vanilla ES6+ JavaScript, CSS3 (Glassmorphism), HTML5
- **Security**: SHA-256 Password Hashing & UUID Session Management
- **Desktop Host**: Java Swing / AWT Control Panel

---

## 🚀 Key Features

| Feature | Description |
| :--- | :--- |
| **📊 Smart Dashboard** | Real-time tracking of revenue, stock levels, and daily performance metrics. |
| **📦 Pro Inventory** | One-click stock updates, low-stock alerts, and category management. |
| **👥 CRM System** | Detailed customer profiles with nested purchase and transaction history. |
| **🧾 Atomic Billing** | Transactional integrity via JDBC; stock updates only if billing succeeds. |
| **🔒 Secure Auth** | Multi-user support with encrypted credentials and session persistence. |

---

## 📸 Feature Showcases

<div align="center">
  <table border="0">
    <tr>
      <td width="50%" align="center"><b>Inventory Management</b><br><img src="previews/preview_products.png"></td>
      <td width="50%" align="center"><b>Customer CRM</b><br><img src="previews/preview_customers.png"></td>
    </tr>
    <tr>
      <td width="50%" align="center"><b>Smart Billing Terminal</b><br><img src="previews/preview_billing.png"></td>
      <td width="50%" align="center"><b>Transactional History</b><br><img src="previews/preview_history.png"></td>
    </tr>
  </table>
</div>

---

## 🛠️ Getting Started

### Prerequisites
- **Java JDK 11 or higher**
- **MySQL Server** (if running locally)

### Setup & Installation
1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/pranav662/Store-Management-System-Java.git
    cd Store-Management-System-Java
    ```
2.  **Run the Automator**:
    Double-click `run.bat` or execute via terminal:
    ```cmd
    run.bat
    ```
    *This will auto-download dependencies, compile source code, and launch the Control Panel.*

3.  **Start the Server**:
    - Click **▶ Start Server** in the window.
    - Visit `http://localhost:8080` to log in.

---

## ⚙️ Environment Variables

For deployment (Railway/Docker), set these variables:

| Variable | Required | Example |
| :--- | :--- | :--- |
| `MYSQL_URL` | Yes | `jdbc:mysql://host:port/dbname` |
| `MYSQLUSER` | Yes | `root` |
| `MYSQLPASSWORD` | Yes | `your_secure_password` |
| `PORT` | No | `8080` (Default) |

---

## 📜 License
Licensed under the **MIT License**. See [LICENSE](LICENSE) for details.

---
Built with ☕ and ❤️ by [Pranav](https://github.com/pranav662)
