import { AuthProvider } from 'react-admin';

const authProvider: AuthProvider = {
  // Called when the user attempts to log in
  login: ({ username, password }) => {
    // Accept any username/password for demo purposes
    localStorage.setItem('auth', JSON.stringify({ username, role: 'admin' }));
    return Promise.resolve();
  },

  // Called when the user clicks on the logout button
  logout: () => {
    localStorage.removeItem('auth');
    return Promise.resolve();
  },

  // Called when the API returns an error
  checkError: ({ status }: { status: number }) => {
    if (status === 401 || status === 403) {
      localStorage.removeItem('auth');
      return Promise.reject();
    }
    return Promise.resolve();
  },

  // Called when the user navigates to a new location, to check for authentication
  checkAuth: () => {
    return localStorage.getItem('auth')
      ? Promise.resolve()
      : Promise.reject();
  },

  // Called when the user navigates to a new location, to check for permissions / roles
  getPermissions: () => {
    const auth = localStorage.getItem('auth');
    return auth ? Promise.resolve(JSON.parse(auth).role) : Promise.reject();
  },

  // Optional: Return user identity
  getIdentity: () => {
    const auth = localStorage.getItem('auth');
    if (auth) {
      const { username } = JSON.parse(auth);
      return Promise.resolve({
        id: username,
        fullName: username,
        avatar: `https://ui-avatars.com/api/?name=${username}&background=4CAF50&color=fff`,
      });
    }
    return Promise.reject();
  },
};

export default authProvider;
