// The file contents for the current environment will overwrite these during build.
// The build system defaults to the dev environment which uses 'environment.ts', but if you do
// 'ng build --env=prod' then 'environment.prod.ts' will be used instead.
// The list of which env maps to which file can be found in '.angular-cli.json'.

export const environment = {
  production: false,

  yauth_default: 'https://api.login.yahoo.com/oauth2/request_auth?response_type=code&language=en-us' +
  '&client_id=dj0yJmk9NEJVRHRaRnpWa09SJmQ9WVdrOVREQktiREUzTjJrbWNHbzlNQS0tJnM9Y29uc3VtZXJzZWNyZXQmeD1iYQ--' +
  '&redirect_uri=https%3A%2F%2Fhairball.herokuapp.com%2Findex.html%3Froute%3Df%2Fshopify%2Fews',

  ewsBaseUrl: 'https://localhost:4443',
  appTitle: 'promote your products and bring users to your site',
  geminiAcctInvalid: 'A Yahoo account you used to sign in has no Gemini access.',
  geminiAcctInactive: 'Your Yahoo Gemini account is inactive. Please click the contact link at top left to reach out to Oath for support.',
  geminiMaxBidPrice: 'Maximum bid is ${price}. To bid higher, raise budget to 50 times your desired bid.',
  geminiMinBidPrice: 'Minimum bid is $0.05',
  geminiMinBidBudget: 'Minimum budget is $5.00',
  geminiHomeUrl: 'https://gemini.yahoo.com/advertiser/home',
  geminiSigInMessage: 'You will be redirected to Yahoo\'s login page, and then automatically brought back here once you\'re done.',
  geminiSigUpMessage: `You will be redirected to Yahoo Gemini page to create a new account.
                          Make sure that you complete your billing info as well because otherwise your product ads will not be served.`,
  geminiUpdateSuccessful: 'Successfully updated.',
  geminiStartSuccessful: 'Successfully started.',
  geminiStopSuccessful: 'Successfully stopped.'
};
