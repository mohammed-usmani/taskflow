module.exports = {
  testEnvironment: '<rootDir>/jest.environment.cjs',      // jsdom (fake browser DOM) + missing web APIs
  setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
  moduleNameMapper: { '\\.css$': 'identity-obj-proxy' },   // components may import CSS; Jest can't parse it
  // Jest skips node_modules when compiling, but react-router and two of its dependencies
  // ship only ES modules (some as .mjs), which Jest's CommonJS runtime can't load.
  // Compile those through Babel too.
  transform: { '^.+\\.(m|c)?[jt]sx?$': 'babel-jest' },
  transformIgnorePatterns: ['/node_modules/(?!(react-router|@remix-run|cookie-es)/)'],
}
