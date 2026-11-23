import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    files: ["app/**/*.{ts,tsx}"],
    ignores: ["app/services/**", "**/*.test.{ts,tsx}", "**/__tests__/**"],
    rules: {
      "no-restricted-syntax": [
        "error",
        {
          selector:
            "Literal[value=/^https?:\\/\\/(localhost|127\\.0\\.0\\.1)/]",
          message: "Use API_BASE_URL from app/services/itineraryApi.ts.",
        },
      ],
    },
  },
  {
    files: ["app/page.tsx", "app/layout.tsx"],
    rules: {
      "no-restricted-syntax": [
        "error",
        {
          selector: "Program > ExpressionStatement[directive='use client']",
          message: "page.tsx and layout.tsx must stay Server Components.",
        },
      ],
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
    "jest.setup.js",
  ]),
]);

export default eslintConfig;
