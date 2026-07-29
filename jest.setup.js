require("@testing-library/jest-dom");

// Mock TextDecoder / TextEncoder if needed in jsdom environment
if (typeof global.TextDecoder === "undefined") {
  const { TextDecoder, TextEncoder } = require("util");
  global.TextDecoder = TextDecoder;
  global.TextEncoder = TextEncoder;
}

// Mock scrollIntoView for jsdom
Element.prototype.scrollIntoView = jest.fn();
