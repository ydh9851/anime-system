const KEY = 'aniscope-theme'
export function applyTheme(t) {
  document.documentElement.classList.toggle('dark', t === 'dark')
}
export function currentTheme() {
  return localStorage.getItem(KEY) || 'light'
}
export function toggleTheme() {
  const t = currentTheme() === 'dark' ? 'light' : 'dark'
  localStorage.setItem(KEY, t)
  applyTheme(t)
  return t
}
