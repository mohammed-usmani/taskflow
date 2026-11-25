// Used only by Jest (Vite compiles the app itself). Turns TS + JSX + ES modules into
// CommonJS that Jest can run.

// `import.meta` exists only in ES modules and has no CommonJS equivalent, so any file
// using it can't be converted. react-router reads `import.meta.hot` (Vite's hot reload).
// Under Jest there is no Vite, so replace it with an empty object.
const importMetaAsEmptyObject = () => ({
  visitor: {
    MetaProperty(path) {
      if (path.node.meta.name === 'import') path.replaceWithSourceString('({ env: {} })')
    },
  },
})

module.exports = {
  presets: [
    ['@babel/preset-env', { targets: { node: 'current' }, modules: 'commonjs' }],
    ['@babel/preset-react', { runtime: 'automatic' }],
    '@babel/preset-typescript',
  ],
  plugins: [importMetaAsEmptyObject],
}
