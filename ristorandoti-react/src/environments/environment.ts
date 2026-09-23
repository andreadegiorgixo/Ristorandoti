export const environment = {
  production: true,
  // Base URL delle API Spring Boot in produzione
  apiUrl: 'https://api.ristorandoti.it/api',
  // Client ID OAuth di Google (Google Cloud Console → Credenziali → "Applicazione web").
  // Deve coincidere con app.google.client-id del backend. Se vuoto il pulsante Google è disabilitato.
  googleClientId: '',
};
