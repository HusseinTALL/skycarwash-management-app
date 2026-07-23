/* ESLint config — Vue 3 + Vite (ESLint 8, eslintrc format) */
module.exports = {
  root: true,
  env: {
    browser: true,
    es2022: true,
    node: true,
  },
  extends: [
    'eslint:recommended',
    'plugin:vue/vue3-essential',
  ],
  parserOptions: {
    ecmaVersion: 'latest',
    sourceType: 'module',
  },
  rules: {
    // SFCs are identified by filename; single-word view/component files are fine here.
    'vue/multi-word-component-names': 'off',
    // Keep unused vars as a non-blocking signal; allow deliberately-ignored args.
    'no-unused-vars': ['warn', { args: 'none', ignoreRestSiblings: true }],
    // Best-effort blocks (e.g. optional localStorage writes) legitimately use `catch {}`.
    'no-empty': ['error', { allowEmptyCatch: true }],
    // Some modals are mounted/unmounted by a parent v-if; surface as a warning, not a blocker.
    'vue/require-toggle-inside-transition': 'warn',
  },
  overrides: [
    {
      // Vitest exposes these as globals (test.globals = true)
      files: ['src/__tests__/**', '**/*.test.js', '**/*.spec.js'],
      globals: {
        describe: 'readonly',
        it: 'readonly',
        test: 'readonly',
        expect: 'readonly',
        vi: 'readonly',
        beforeEach: 'readonly',
        afterEach: 'readonly',
        beforeAll: 'readonly',
        afterAll: 'readonly',
      },
    },
  ],
}
