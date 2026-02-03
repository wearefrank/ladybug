import typescriptParser from '@typescript-eslint/parser';
import prettierPlugin from 'eslint-plugin-prettier';
import tsPlugin from '@typescript-eslint/eslint-plugin';
import angularPlugin from '@angular-eslint/eslint-plugin';
import eslintPluginPrettierRecommended from 'eslint-plugin-prettier/recommended';
import eslintPluginUnicorn from 'eslint-plugin-unicorn';
import js from '@eslint/js';
import eslintConfigPrettier from 'eslint-config-prettier';

export default [
  {
    ignores: [
      '.cache/',
      '.git/',
      '.github/',
      'node_modules/',
      'dist/',
      'target/',
      'karma.conf.js',
      '.angular/',
      './cypress',
    ],
  },
  {
    files: ['**/*.ts'],
    languageOptions: {
      parser: typescriptParser,
      parserOptions: {
        project: ['./tsconfig.json', './tsconfig.app.json', './tsconfig.spec.json'],
      },
    },
    plugins: {
      '@typescript-eslint': tsPlugin,
      '@angular-eslint': angularPlugin,
      prettier: prettierPlugin,
    },
    rules: {
      // TypeScript: https://typescript-eslint.io/rules/
      ...tsPlugin.configs.recommended.rules,
      ...tsPlugin.configs.stylistic.rules,
      '@typescript-eslint/explicit-function-return-type': 'error',
      '@typescript-eslint/triple-slash-reference': 'warn',
      '@typescript-eslint/no-unused-vars': [
        'error',
        {
          args: 'all',
          argsIgnorePattern: '^_',
          caughtErrors: 'all',
          caughtErrorsIgnorePattern: '^_',
          destructuredArrayIgnorePattern: '^_',
          varsIgnorePattern: '^_',
          ignoreRestSiblings: true,
        },
      ],
      '@typescript-eslint/switch-exhaustiveness-check': 'error',
      // Angular: https://github.com/angular-eslint/angular-eslint/blob/main/packages/eslint-plugin/README.md
      ...angularPlugin.configs.recommended.rules,
      '@angular-eslint/directive-selector': ['error', { type: 'attribute', prefix: 'app', style: 'camelCase' }],
      '@angular-eslint/component-selector': ['error', { type: 'element', prefix: 'app', style: 'kebab-case' }],

      // EcmaScript: https://eslint.org/docs/latest/rules/
      ...js.configs.recommended.rules,
      'prefer-template': 'error',
      'no-undef': 'off',
      'no-unused-vars': 'off', // Handled by @typescript-eslint/no-unused-vars

      // Prettier: https://github.com/prettier/eslint-config-prettier?tab=readme-ov-file#special-rules
      ...eslintConfigPrettier.rules,
      'prettier/prettier': 'warn',
    },
  },
  // Unicorn
  eslintPluginUnicorn.configs.recommended,
  {
    rules: {
      'unicorn/prevent-abbreviations': 'warn',
      'unicorn/no-array-reduce': 'off',
      'unicorn/prefer-ternary': 'warn',
      'unicorn/no-null': 'off',
      'unicorn/prefer-dom-node-text-content': 'warn',
    },
  },
  eslintPluginPrettierRecommended,
];
