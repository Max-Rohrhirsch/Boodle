# Start the Application
```bash
docker compose up --build
```

# Environment Variables
Rename the `.env.example` into `.env` and change the keys!

# API / Login
- Login endpoint: `POST /api/auth/login`
- Public endpoint for registration: `POST /api/users`
- Other endpoints require JWT authentication.

All Endpoints can be found on `http://localhost:8080/swagger-ui/index.html`.

# Frontend
- Open: `http://localhost`
- Simple login page is available at route `/login`.
- On successful login, the frontend stores the JWT and calls `GET /api/users`.

# Database Reset (when schema changed)
If you changed schema-relevant backend code and want a clean restart:

```bash
docker compose down -v
docker compose up --build
```

# To-DO:
- [X] Make JTW work
- [ ] Create some simple Tests
- [ ] Create Database Tables
- [ ] Create Tests for them. (Only integration tests, because there is almost no logik)
- [ ] Create frontend for login and header
- [ ] Create frontend for User and Kurs managment
- [ ] Check in Backend if User has enauth rights
- [ ] Create frontend for Stundenplan and Kurs
- [ ] Replan
- [ ] Make if more customizable
