// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses `environment.ts`, but if you do
// `ng build --env=prod` then `environment.prod.ts` will be used instead.
// The list of which env maps to which file can be found in `.angular-cli.json`.

export const environment = {
  production: false,
  ewsBaseUrl: 'http://localhost:4088/',

  oauthUrl: 'https://api.login.yahoo.com/oauth2/request_auth?response_type=code&language=en-us&client_id=' +
  'dj0yJmk9NEJVRHRaRnpWa09SJmQ9WVdrOVREQktiREUzTjJrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iYQ--&redirect_uri=' +
  'https%3A%2F%2Fhairball.herokuapp.com%2Fg%2Fshopify%2Fews%3F_mc%3D0bc7f4694ab72663057da0eb5f14d52b%26shop%3Ddpa-bridge.myshopify.com',
  geminiHomeUrl: 'https://gemini.yahoo.com/advertiser/home',
  geminiSigInMessage: 'You will be redirected to Yahoo\'s login page, and brought back here after you\'re done. Do not close your Shopify screen if it is open',
  geminiSigUpMessage: 'You will be redirected to Yahoo Gemini page to create a new account. Make sure that you complete your billing info as well because otherwise your product ads will not run.'
};
