const KEY = 'filter_partner';

export function defaultPartnerFilter() {
  return {
    distance: 'all',
    gender: 'all',
    partnerType: 'all',
    matchLevel: 'all',
    partnerStatus: 'all'
  };
}

export function loadPartnerFilter() {
  try {
    const raw = localStorage.getItem(KEY);
    if (!raw) {
      return defaultPartnerFilter();
    }
    return { ...defaultPartnerFilter(), ...JSON.parse(raw) };
  } catch {
    return defaultPartnerFilter();
  }
}

export function savePartnerFilter(filter) {
  localStorage.setItem(KEY, JSON.stringify(filter));
}
