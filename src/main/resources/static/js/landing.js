// Scroll reveal
const obs = new IntersectionObserver(es => es.forEach(e => { if(e.isIntersecting) e.target.classList.add('visible'); }), {threshold:0.1,rootMargin:'0px 0px -40px 0px'});
document.querySelectorAll('.reveal').forEach(el => obs.observe(el));

// Nav background on scroll
window.addEventListener('scroll', () => {
  document.getElementById('navbar').style.background = window.scrollY > 30 ? 'rgba(8,12,26,0.97)' : 'rgba(8,12,26,0.8)';
});

// Animated counters
function counter(el) {
  const target = +el.dataset.count, dur = 1600, s = performance.now();
  const lbl = el.closest('.si')?.querySelector('.si-lbl')?.textContent || el.parentElement?.querySelector('.hstat-lbl')?.textContent || '';
  const suffix = lbl.includes('%') ? '%' : (lbl.includes('Min') ? ' min' : '+');
  (function step(now) {
    const p = Math.min((now - s) / dur, 1), ease = 1 - Math.pow(1 - p, 3), v = Math.round(ease * target);
    el.textContent = v >= 1000 ? v.toLocaleString() : v;
    if (p < 1) requestAnimationFrame(step);
    else el.textContent = (target >= 1000 ? target.toLocaleString() : target) + suffix;
  })(s);
}
const cobs = new IntersectionObserver(es => es.forEach(e => {
  if(e.isIntersecting && e.target.dataset.count) { counter(e.target); cobs.unobserve(e.target); }
}), {threshold:0.5});
document.querySelectorAll('[data-count]').forEach(el => cobs.observe(el));

// Smooth scroll for anchor links
document.querySelectorAll('a[href^="#"]').forEach(a => a.addEventListener('click', e => {
  const t = document.querySelector(a.getAttribute('href'));
  if(t) { e.preventDefault(); t.scrollIntoView({behavior:'smooth',block:'start'}); }
}));
