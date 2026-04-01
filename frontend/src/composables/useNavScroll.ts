import { ref } from 'vue'

// Controls the navbar gradient that descends on scroll
export const navGradientHeight = ref(0)
// True when past the hero — navbar becomes solid
export const navScrolled = ref(false)

export function useNavScroll() {
  let ticking = false

  function update() {
    const scrollY = window.scrollY
    const maxScroll = window.innerHeight * 0.8
    // Gradient grows from 0 to 200px as you scroll through the first 80% of viewport
    navGradientHeight.value = Math.min(200, Math.round((scrollY / maxScroll) * 200))
    // Solid navbar when scrolled past hero
    navScrolled.value = scrollY > window.innerHeight * 0.15
    ticking = false
  }

  function onScroll() {
    if (!ticking) {
      requestAnimationFrame(update)
      ticking = true
    }
  }

  function init() {
    update()
    window.addEventListener('scroll', onScroll, { passive: true })
  }

  function destroy() {
    window.removeEventListener('scroll', onScroll)
  }

  return { init, destroy, navGradientHeight, navScrolled }
}

