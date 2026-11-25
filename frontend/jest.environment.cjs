const { TestEnvironment } = require('jest-environment-jsdom')

/**
 * jsdom (Jest's fake browser) is missing web APIs that real browsers and Node both have,
 * and react-router uses them on every navigation. Copy Node's versions in.
 * AbortController/AbortSignal come from Node too, because Node's Request only accepts
 * Node's own AbortSignal.
 */
module.exports = class BrowserLikeEnvironment extends TestEnvironment {
  constructor(...args) {
    super(...args)
    const { Request, Response, Headers, fetch, AbortController, AbortSignal, TextEncoder, TextDecoder } = globalThis
    Object.assign(this.global, { Request, Response, Headers, fetch, AbortController, AbortSignal, TextEncoder, TextDecoder })
  }
}
