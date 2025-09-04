# LuckXpress Backend

LuckXpress Backend is a Node.js/TypeScript Express API server that provides backend services for the LuckXpress application.

**Always reference these instructions first and fallback to search or bash commands only when you encounter unexpected information that does not match the info here.**

## Working Effectively

### Prerequisites and Setup
- Node.js v20.19.4 or later is required (already available in environment)
- npm 10.8.2 or later is required (already available in environment)
- The repository uses TypeScript with Express.js for the backend API

### Bootstrap, Build, and Test the Repository

**CRITICAL: All commands below have been validated to work. NEVER CANCEL long-running commands.**

1. **Initial Setup** (takes ~30 seconds):
   ```bash
   npm install
   ```
   - NEVER CANCEL: Wait for npm install to complete, can take up to 30 seconds
   - Installs all dependencies including TypeScript, ESLint, Jest, and Express

2. **Build the Application** (takes ~1-2 seconds):
   ```bash
   npm run build
   ```
   - NEVER CANCEL: Set timeout to 60+ seconds, though build usually completes in under 2 seconds
   - Compiles TypeScript to JavaScript in the `dist/` directory
   - Creates `dist/index.js`, `dist/server.js`, and `dist/index.test.js`

3. **Run Tests** (takes ~2-3 seconds, but Jest hangs for 10+ seconds):
   ```bash
   timeout 10s npm test
   ```
   - NEVER CANCEL: Set timeout to 30+ seconds for the timeout command
   - Tests pass successfully but Jest hangs waiting for server cleanup
   - Use `timeout 10s` to prevent Jest from hanging indefinitely
   - Output shows "1 passed, 1 total" - this means tests are working correctly

4. **Code Quality** (takes ~1-2 seconds):
   ```bash
   npm run lint
   ```
   - NEVER CANCEL: Set timeout to 30+ seconds
   - Runs ESLint on all .js, .jsx, .ts, .tsx files
   - Configuration supports both JavaScript and TypeScript
   - SARIF output for GitHub Actions: `npm run lint:sarif`

### Run the Application

**Development Mode** (with TypeScript hot reload):
```bash
npm run dev
```
- Starts server on port 3000 using ts-node
- NEVER CANCEL: Server runs indefinitely until stopped
- Use Ctrl+C or timeout to stop: `timeout 5s npm run dev`

**Production Mode** (compiled JavaScript):
```bash
npm start
```
- Requires `npm run build` to be run first
- Starts compiled server from `dist/server.js`
- NEVER CANCEL: Server runs indefinitely until stopped
- Use Ctrl+C or timeout to stop: `timeout 5s npm start`

### Manual Validation
- **ALWAYS test the API endpoint after making changes**:
  1. Start the server: `timeout 10s npm run dev &`
  2. Test the endpoint: `curl http://localhost:3000` (may fail if server not ready)
  3. Expected response: `{"message":"Welcome to LuckXpress Backend API"}`
  4. Stop the server with Ctrl+C

## Project Structure

### Key Files and Directories
```
/
├── index.ts              # Main Express application (no server startup)
├── server.ts             # Server startup file (imports app from index.ts)
├── index.test.ts         # API tests using Jest and Supertest
├── package.json          # Node.js dependencies and scripts
├── tsconfig.json         # TypeScript configuration
├── .eslintrc.js          # ESLint configuration for JS/TS
├── jest.config.js        # Jest testing configuration
├── jest.setup.js         # Jest setup (sets NODE_ENV=test)
├── dist/                 # Compiled JavaScript output
└── .github/workflows/    # GitHub Actions
    └── eslint.yml        # ESLint CI/CD pipeline
```

### Important Configuration Files

**ESLint Setup** (.eslintrc.js):
- Supports both JavaScript and TypeScript
- Uses @typescript-eslint/parser for TypeScript files
- Includes @typescript-eslint plugins
- Run with: `npm run lint`

**TypeScript Setup** (tsconfig.json):
- Target: ES2020, Module: CommonJS
- Output directory: `dist/`
- Strict mode enabled
- Excludes: node_modules, dist

**GitHub Actions**:
- ESLint workflow runs on push/PR to main branch
- Uses eslint@8.10.0 and @microsoft/eslint-formatter-sarif@3.1.0
- Uploads SARIF results for security scanning

## Validation Scenarios

### After Making Code Changes
1. **Build and test your changes**:
   ```bash
   npm run build
   timeout 10s npm test
   npm run lint
   ```

2. **Manual API testing**:
   ```bash
   # Start development server
   timeout 10s npm run dev &
   
   # Test the API (may need to wait a moment for server startup)
   sleep 2 && curl http://localhost:3000
   
   # Stop any background processes
   pkill -f "ts-node\|node"
   ```

3. **Production build testing**:
   ```bash
   npm run build
   timeout 10s npm start &
   sleep 2 && curl http://localhost:3000
   pkill -f "node"
   ```

### Always Run Before Committing
```bash
npm run lint
npm run build
timeout 10s npm test
```
- ESLint must pass (exit code 0)
- Build must succeed (exit code 0)
- Tests must pass (shows "1 passed, 1 total")

## Common Tasks

### Adding New Dependencies
```bash
# Production dependencies
npm install <package-name>

# Development dependencies  
npm install <package-name> --save-dev
```

### Adding New API Endpoints
1. Edit `index.ts` to add new routes
2. Add corresponding tests in `index.test.ts`
3. Run validation commands above

### Debugging
- Use `npm run dev` for development with TypeScript
- Add console.log statements for debugging
- Check server logs on port 3000

## Known Issues and Workarounds

1. **Jest Hanging**: Tests pass but Jest doesn't exit cleanly due to Express server lifecycle
   - **Solution**: Use `timeout 10s npm test` to prevent indefinite hanging
   - Tests still run correctly and show pass/fail status

2. **ESLint Deprecation Warnings**: ESLint 8.x shows deprecation warnings
   - **Workaround**: Warnings can be ignored; ESLint functions correctly
   - This matches the GitHub Actions workflow requirements

3. **Server Startup in Tests**: Server may start during testing
   - **Solution**: App and server are separated (index.ts vs server.ts)
   - Tests import app directly without starting server

## Time Expectations

- **npm install**: 5-30 seconds (NEVER CANCEL - set 60+ second timeout)
- **npm run build**: 1-2 seconds (NEVER CANCEL - set 60+ second timeout)  
- **npm test**: 2-3 seconds + 10 second Jest hang (use timeout 10s npm test)
- **npm run lint**: 1-2 seconds (NEVER CANCEL - set 30+ second timeout)
- **Server startup**: 1-2 seconds (npm run dev or npm start)

## Technology Stack

- **Runtime**: Node.js 20.19.4+
- **Language**: TypeScript (compiled to CommonJS)
- **Web Framework**: Express.js
- **Testing**: Jest with Supertest
- **Code Quality**: ESLint with TypeScript support
- **Build Tool**: TypeScript Compiler (tsc)
- **Development**: ts-node for hot reload