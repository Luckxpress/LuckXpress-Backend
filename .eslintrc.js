module.exports = {
  env: {
    browser: true,
    es2021: true,
    node: true,
    jest: true, // Add Jest globals
  },
  extends: [
    'eslint:recommended',
  ],
  parser: '@typescript-eslint/parser',
  parserOptions: {
    ecmaVersion: 12,
    sourceType: 'module',
  },
  plugins: [
    '@typescript-eslint',
  ],
  rules: {
    // TypeScript specific rules
    '@typescript-eslint/no-unused-vars': 'error',
    '@typescript-eslint/explicit-function-return-type': 'off',
    '@typescript-eslint/explicit-module-boundary-types': 'off',
  },
  overrides: [
    {
      files: ['*.ts', '*.tsx'],
      rules: {
        'no-undef': 'off', // TypeScript handles this
      },
    },
    {
      files: ['**/*.test.js', '**/*.test.ts'],
      env: {
        jest: true,
      },
    },
  ],
};