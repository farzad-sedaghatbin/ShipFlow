# Contributing to ShipFlow

Thank you for your interest in contributing to ShipFlow! This document provides guidelines and instructions for contributing.

> **⚠️ IMPORTANT**: Before starting any development work, read [DEVELOPMENT_WORKFLOW.md](DEVELOPMENT_WORKFLOW.md) for the complete development checklist, testing requirements, branch strategy, and project structure.

## 🌟 Code of Conduct

This project adheres to the [Contributor Covenant Code of Conduct](CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code.

## 🚀 Getting Started

### Prerequisites

- **Java 17+** - [Download OpenJDK](https://adoptium.net/)
- **Node.js 18+** - [Download Node.js](https://nodejs.org/)
- **Docker** (optional) - [Download Docker](https://www.docker.com/)
- **AI Provider** (choose one for AI features):
  - **Ollama** (recommended for development, no API key) - [Download Ollama](https://ollama.ai/)
  - **OpenAI** (production-ready) - Get API key from [OpenAI Platform](https://platform.openai.com/api-keys)
  - **RunPod** (cloud GPU) - Get API key from [RunPod](https://www.runpod.io/)

### Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/farzad-sedaghatbin/shipflow.git
   cd shipflow
   ```

2. **Start the backend**
   ```bash
   cd backend
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

3. **Start the frontend** (in a new terminal)
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

4. **Access the application**
   - Frontend: http://localhost:3000
   - Backend API: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui.html

### Using Docker

> Requires Docker Compose v2 (`docker compose` plugin). The legacy `docker-compose` v1 is not supported.

```bash
docker compose up --build
```

### AI Features Setup (Optional)

ShipFlow supports multiple AI providers. Choose one:

**Option A: Ollama (Local, No API Key)**
1. Install Ollama: https://ollama.ai/
2. Pull the Mistral model:
   ```bash
   ollama pull mistral:instruct
   ollama serve
   ```
3. Create `.env`:
   ```bash
   AI_PROVIDER=ollama
   OLLAMA_BASE_URL=http://localhost:11434
   OLLAMA_MODEL=mistral:instruct
   ```

**Option B: OpenAI ChatGPT (Production)**
1. Get API key from https://platform.openai.com/api-keys
2. Create `.env`:
   ```bash
   AI_PROVIDER=openai
   OPENAI_API_KEY=sk-your-api-key-here
   OPENAI_MODEL=gpt-4-turbo-preview
   ```

**Option C: RunPod (Cloud GPU)**
1. Set up RunPod endpoint and get API key
2. Use `.env.example` as template

See [ENVIRONMENT_SETUP.md](ENVIRONMENT_SETUP.md) for detailed configuration.

## 📝 Making Changes

### Branching Strategy

- `main` - Production-ready code
- `develop` - Integration branch for features
- `feature/*` - New features
- `fix/*` - Bug fixes
- `docs/*` - Documentation updates

### Creating a Branch

```bash
git checkout -b feature/your-feature-name
```

### Commit Messages

We follow [Conventional Commits](https://www.conventionalcommits.org/):

```
<type>(<scope>): <description>

[optional body]

[optional footer]
```

**Types:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation only
- `style` - Formatting, missing semicolons, etc.
- `refactor` - Code change that neither fixes a bug nor adds a feature
- `perf` - Performance improvement
- `test` - Adding missing tests
- `chore` - Maintenance tasks

**Examples:**
```
feat(pitch): add drag-and-drop reordering
fix(auth): resolve JWT token refresh issue
docs(readme): update installation instructions
```

### Versioning and Changelog

This project follows [Semantic Versioning](https://semver.org/).
All notable changes are documented in [CHANGELOG.md](CHANGELOG.md).

## 🧪 Testing

### Backend Tests

```bash
cd backend
./mvnw test
```

**Test Coverage:** We enforce 80%+ line coverage via JaCoCo. Check coverage report:
```bash
./mvnw test jacoco:report
open target/site/jacoco/index.html
```

### Frontend Tests

```bash
cd frontend
npm run test
npm run test:coverage
```

### Running All Tests

```bash
./scripts/run-tests.sh
```

## 📁 Project Structure

```
shipflow/
├── backend/                 # Spring Boot application
│   └── src/
│       ├── main/
│       │   ├── java/       # Java source code
│       │   └── resources/  # Configuration files
│       └── test/           # Test files
├── frontend/               # React application
│   └── src/
│       ├── components/     # Reusable UI components
│       ├── pages/          # Page components
│       ├── services/       # API service layer
│       ├── contexts/       # React contexts
│       └── types/          # TypeScript types
└── scripts/                # Utility scripts
```

## 🔍 Code Style

### Backend (Java)

- Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- Use Lombok annotations to reduce boilerplate
- Write meaningful Javadoc for public APIs
- Keep methods focused and under 30 lines when possible

### Frontend (TypeScript/React)

- Use TypeScript strict mode
- Prefer functional components with hooks
- Use shadcn/ui components with Tailwind CSS
- Follow the existing component patterns

### Accessibility (WCAG 2.1 AA)

All contributions must maintain WCAG 2.1 AA compliance:

- **ARIA Labels**: Add `aria-label` to all buttons and interactive elements
- **Keyboard Navigation**: Ensure all functionality works with keyboard only
- **Focus Indicators**: Use `&:focus-visible` with 2px solid outline
- **Color Contrast**: Use `text.primary` or `text.secondary` (not `text.disabled`) for readable text
- **Semantic HTML**: Use proper landmarks (`<nav>`, `<main>`, `<section>`)
- **Screen Readers**: Add `aria-hidden="true"` to decorative icons
- **Loading States**: Use `aria-busy`, `aria-live="polite"`, and `role="status"`
- **Dialogs**: Include `aria-labelledby` and `aria-describedby`
- **Forms**: Mark required fields with `aria-required` and link helper text with `aria-describedby`

## 📤 Submitting Changes

### Pull Request Process

1. **Update your branch** with the latest from `main`:
   ```bash
   git fetch origin
   git rebase origin/main
   ```

2. **Run tests** and ensure they pass:
   ```bash
   ./scripts/run-tests.sh
   ```

3. **Create a Pull Request** with:
   - Clear title following commit convention
   - Description of changes
   - Link to related issue (if any)
   - Screenshots for UI changes

4. **Address review feedback** promptly

5. **Squash commits** if requested before merge

### PR Checklist

- [ ] Tests pass locally
- [ ] Code follows project style guidelines
- [ ] Documentation updated (if needed)
- [ ] Commit messages follow convention
- [ ] No console.log or debug statements left
- [ ] UI changes tested in both light and dark mode
- [ ] Accessibility: ARIA labels, keyboard navigation, focus indicators

## 🐛 Reporting Issues

### Bug Reports

When reporting bugs, include:
- Clear description of the bug
- Steps to reproduce
- Expected vs actual behavior
- Screenshots (if applicable)
- Environment details

### Feature Requests

When requesting features, include:
- Problem you're trying to solve
- Proposed solution
- Alternatives considered

## 🎯 Good First Issues

Look for issues labeled `good first issue` - these are great for newcomers!

### Examples of Good First Issues

Here are some examples of the types of contributions that make great first issues:

#### 🎨 Frontend (React/TypeScript)
- **Add loading skeletons** - Replace loading spinners with skeleton screens for better UX
- **Improve accessibility** - Add ARIA labels, keyboard navigation, screen reader support
- **Add form validation messages** - Better inline error messages for forms
- **Create new illustrations** - Add SVG illustrations for empty states
- **Add tooltips** - Add helpful tooltips to icons and buttons
- **Improve mobile responsiveness** - Fix layout issues on smaller screens
- **Add keyboard shortcuts** - Implement keyboard shortcuts for common actions
- **Dark mode fixes** - Fix components that don't render well in dark mode

#### 🔧 Backend (Java/Spring Boot)
- **Add input validation** - Add `@Valid` annotations and custom validators
- **Improve error messages** - Make API error responses more descriptive
- **Add logging** - Add appropriate log statements for debugging
- **Write unit tests** - Increase test coverage for existing services
- **Add API documentation** - Improve Swagger/OpenAPI descriptions
- **Add pagination** - Add pagination to list endpoints that don't have it

#### 📝 Documentation
- **Fix typos** - Correct spelling and grammar in docs
- **Add code comments** - Document complex functions and algorithms
- **Improve README** - Add more examples, clarify setup instructions
- **Add JSDoc/Javadoc** - Document public APIs
- **Create tutorials** - Write guides for common use cases
- **Translate documentation** - Help translate docs to other languages

#### 🧪 Testing
- **Add missing tests** - Write tests for untested components/services
- **Add integration tests** - Test API endpoints end-to-end
- **Add E2E tests** - Write Cypress/Playwright tests for critical flows
- **Improve test data** - Create better test fixtures and factories

#### 🐛 Bug Fixes
- **Fix console warnings** - Resolve React warnings in browser console
- **Fix type errors** - Resolve TypeScript strict mode issues
- **Fix edge cases** - Handle empty states, null values, edge cases

### How to Find Good First Issues

1. Browse the `good first issue` label in the Issues tab
2. Comment on an issue to express interest
3. Wait for a maintainer to assign it to you
4. Ask questions if anything is unclear!

## 💬 Getting Help

- Open a Discussion on GitHub
- Check existing Issues
- Read the [Documentation](README.md)

## 📜 License

By contributing, you agree that your contributions will be licensed under the MIT License.

---

Thank you for contributing to ShipFlow! 🎉
