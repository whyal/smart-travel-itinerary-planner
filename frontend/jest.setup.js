require("@testing-library/jest-dom");

if (typeof global.TextDecoder === "undefined") {
  const { TextDecoder, TextEncoder } = require("util");
  global.TextDecoder = TextDecoder;
  global.TextEncoder = TextEncoder;
}

Element.prototype.scrollIntoView = jest.fn();
