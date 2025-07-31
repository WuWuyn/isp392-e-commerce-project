<div align="center">

  <!-- **Note:** You should replace the line below with your project's logo -->
  <img src="uploads/settings/1753502014447_5acb27f7-dcb8-4aec-b480-890705a48304.jpg" alt="ReadHub Logo"/>

  <h1>ReadHub</h1>

  <p>
    <strong>A multi-vendor e-commerce platform for book enthusiasts.</strong>
  </p>

  <p>
    <a href="#"><img alt="Status" src="https://img.shields.io/badge/status-in%20progress-yellow"/></a>
    <a href="/LICENSE"><img alt="License" src="https://img.shields.io/badge/license-MIT-blue"/></a>
    <a href="#"><img alt="Java" src="https://img.shields.io/badge/Java-11+-ED8B00?logo=openjdk&logoColor=white"/></a>
    <a href="#"><img alt="Spring Boot" src="https://img.shields.io/badge/Spring_Boot-2.5.x-6DB33F?logo=spring&logoColor=white"/></a>
  </p>

</div>

**ReadHub** is a multi-vendor e-commerce platform designed exclusively for the book-loving community. Our mission is to create an online ecosystem where readers can easily discover, purchase, and manage books from a diverse range of vendors. The project integrates advanced features like an AI-powered chatbot to deliver the best possible interactive and supportive user experience.

As an open-source project, ReadHub warmly welcomes contributions from developers to help build and perfect the platform together.

## ✨ Key Features

| Icon | Feature | Description |
| :---: | --- | --- |
| 🏪 | **Multi-Vendor Support** | Allows multiple booksellers to register, list their products, and manage their own inventory. |
| 🤖 | **AI Chatbot Integration** | An intelligent chatbot provides instant support, book recommendations, and query resolution 24/7. |
| 📚 | **Rich Book Catalog** | A comprehensive, searchable database of books, organized by genre, author, and user ratings. |
| 🛒 | **Shopping Cart & Checkout** | An optimized cart and checkout process ensures smooth and secure transactions. |
| ⭐ | **Reviews & Ratings** | Users can leave feedback and ratings, building a community-driven recommendation system. |
| 🔐 | **User Authentication** | Secure mechanisms for registration, login, and profile management to deliver personalized experiences. |
| 📊 | **Admin & Vendor Dashboards** | Dedicated interfaces for Admins to oversee operations and for Vendors to manage listings and orders. |
| 📱 | **Responsive Design** | A fully responsive UI ensures a seamless experience on all devices, from desktops to mobile phones. |
| 🔍 | **Advanced Search & Filtering**| Powerful search tools with detailed filters for price, publication date, vendor, and more. |

## 🛠️ Tech Stack

| Area | Technologies                                                                         |
| --- |--------------------------------------------------------------------------------------|
| **Frontend** | 🌐 `HTML5`, `CSS3`, `JavaScript`, `Bootstrap`                                        |
| **Backend** | ☕ `Java 17`, `Spring Boot`                                                           |
| **Database** | 🗃️ `SQL Server` / `PostgreSQL`                                                      |
| **Tools & Integrations** | 🔧 `Git`, `Maven`, `Tomcat`, `VNPAY API` (for payments), `Google AI Studio` (for AI) |

## 🚀 Get Started!

Follow these steps to set up a local copy of the project on your machine.

### 1. Prerequisites

*   **Java JDK**: Version 11 or higher.
*   **Maven**: Project build and dependency management tool.
*   **Git**: Version control system.
*   **Database**: A running instance of MySQL or PostgreSQL.

### 2. Installation Guide

1.  **Clone the repository to your machine:**
    ```sh
    git clone https://github.com/WuWuyn/isp392-e-commerce-project.git
    cd isp392-e-commerce-project
    ```

2.  **Install dependencies:**
    Use Maven to download the required libraries.
    ```sh
    mvn clean install
    ```

3.  **Configure the Database:**
    *   Create a new schema in your database (e.g., `readhub_db`).
    *   Open the `src/main/resources/application.properties` file.
    *   Update the `spring.datasource.url`, `username`, and `password` properties accordingly.

4.  **Run the application:**
    Start the server using the Spring Boot command.
    ```sh
    mvn spring-boot:run
    ```

5.  **Access the platform:**
    Open your browser and navigate to `http://localhost:8080`. Congratulations, you're all set!

## 🤝 Contributing

We greatly welcome contributions from the community! If you have an idea for a new feature, want to fix a bug, or improve the documentation, please read our `CONTRIBUTING.md` file for details on the process.

The easiest way to contribute is:
1.  **Fork** the project.
2.  Create a new **Branch** (`git checkout -b feature/AmazingFeature`).
3.  **Commit** your changes (`git commit -m 'Add some AmazingFeature'`).
4.  **Push** to the Branch (`git push origin feature/AmazingFeature`).
5.  Open a **Pull Request**.

## 👥 Contributors

A heartfelt thank you to all the people who have dedicated their time and effort to contribute to this project.

*   **[WuWuyn](https://github.com/WuWuyn)**
*   **[Nho_oi](https://github.com/Cinhode)**
*   **[carotxanh](https://github.com/carotxanh)**

## 📜 License

This project is licensed under the **MIT License**. See the `LICENSE` file for details.