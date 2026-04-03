<script setup lang="ts">
import { computed, useAttrs } from 'vue'
import { RouterLink } from 'vue-router'

defineOptions({ inheritAttrs: false })

type MotionButtonVariant = 'primary' | 'secondary'
type MotionButtonSize = 'md' | 'lg'

const props = withDefaults(
  defineProps<{
    label: string
    to?: string
    href?: string
    target?: string
    rel?: string
    variant?: MotionButtonVariant
    size?: MotionButtonSize
    block?: boolean
  }>(),
  {
    variant: 'primary',
    size: 'md',
    block: false,
  }
)

const attrs = useAttrs()

const renderedChars = computed(() =>
  Array.from(props.label).map((char, index) => ({
    key: `${index}-${char}`,
    value: char === ' ' ? '\u00A0' : char,
  }))
)

const componentTag = computed(() => {
  if (props.href) return 'a'
  if (props.to) return RouterLink
  return 'button'
})

const componentProps = computed(() => {
  if (props.href) {
    const resolvedTarget = props.target ?? '_self'
    return {
      href: props.href,
      target: resolvedTarget,
      rel: props.rel ?? (resolvedTarget === '_blank' ? 'noopener noreferrer' : undefined),
    }
  }

  if (props.to) {
    return { to: props.to }
  }

  return { type: 'button' as const }
})

const mergedProps = computed(() => ({
  ...componentProps.value,
  ...attrs,
}))

const classes = computed(() => [
  'motion-btn',
  `motion-btn--${props.variant}`,
  `motion-btn--${props.size}`,
  props.block ? 'motion-btn--block' : '',
])
</script>

<template>
  <component :is="componentTag" v-bind="mergedProps" :class="classes">
    <span class="motion-btn__bg" />
    <span class="motion-btn__sr">{{ label }}</span>

    <span class="motion-btn__label" aria-hidden="true">
      <span class="motion-btn__row motion-btn__row--top">
        <span v-for="char in renderedChars" :key="`top-${char.key}`" class="motion-btn__char">{{ char.value }}</span>
      </span>
      <span class="motion-btn__row motion-btn__row--bottom">
        <span v-for="char in renderedChars" :key="`bottom-${char.key}`" class="motion-btn__char">{{ char.value }}</span>
      </span>
    </span>
  </component>
</template>

<style scoped>
.motion-btn {
  --motion-border: rgba(53, 98, 122, 0.3);
  --motion-bg: linear-gradient(135deg, #1a4a61, #35627a);
  --motion-bg-hover: linear-gradient(135deg, #e5aea9, #f5f5f5);
  --motion-text: #ffffff;
  --motion-text-hover: #102f43;

  position: relative;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.75rem;
  border: 1px solid var(--motion-border);
  color: var(--motion-text);
  background: rgba(255, 255, 255, 0.03);
  overflow: hidden;
  transition: transform 220ms ease, box-shadow 220ms ease, border-color 220ms ease;
}

.motion-btn--block {
  width: 100%;
}

.motion-btn--md {
  min-height: 2.75rem;
  padding: 0.75rem 1.2rem;
}

.motion-btn--lg {
  min-height: 3.05rem;
  padding: 0.88rem 1.4rem;
}

.motion-btn--primary {
  --motion-border: rgba(26, 74, 97, 0.48);
  --motion-bg: linear-gradient(135deg, #1a4a61, #35627a);
  --motion-bg-hover: linear-gradient(135deg, #e5aea9, #f5f5f5);
  --motion-text: #ffffff;
  --motion-text-hover: #102f43;
}

.motion-btn--secondary {
  --motion-border: rgba(53, 98, 122, 0.34);
  --motion-bg: linear-gradient(135deg, rgba(245, 245, 245, 0.95), rgba(166, 169, 208, 0.5));
  --motion-bg-hover: linear-gradient(135deg, #e5aea9, #f5f5f5);
  --motion-text: #14384d;
  --motion-text-hover: #102f43;
}

.motion-btn__bg {
  position: absolute;
  inset: 0;
  background: var(--motion-bg);
  transform: scale(1);
  transition: transform 240ms cubic-bezier(0.2, 0.8, 0.2, 1), filter 240ms ease;
}

.motion-btn--secondary .motion-btn__bg {
  backdrop-filter: blur(5px);
}

.motion-btn__label {
  position: relative;
  z-index: 1;
  height: 1.25em;
  display: inline-grid;
  place-items: center;
  overflow: hidden;
  font-family: 'Bebas Neue', sans-serif;
  letter-spacing: 0.06em;
  line-height: 1.1;
  font-size: 1.08rem;
}

.motion-btn__row {
  grid-area: 1 / 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.motion-btn__row--top {
  color: var(--motion-text);
}

.motion-btn__row--bottom {
  color: var(--motion-text-hover);
}

.motion-btn__char {
  display: inline-block;
  transform: translateY(0);
  transition: transform 320ms cubic-bezier(0.28, 0.84, 0.42, 1);
  will-change: transform;
}

.motion-btn__row--bottom .motion-btn__char {
  transform: translateY(120%);
}

.motion-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 26px rgba(53, 98, 122, 0.34);
  border-color: rgba(229, 174, 169, 0.65);
}

.motion-btn:hover .motion-btn__bg {
  background: var(--motion-bg-hover);
  transform: scale(1.04);
  filter: saturate(1.1);
}

.motion-btn:hover .motion-btn__row--top .motion-btn__char {
  transform: translateY(-120%);
}

.motion-btn:hover .motion-btn__row--bottom .motion-btn__char {
  transform: translateY(0);
}

.motion-btn:disabled {
  opacity: 0.58;
  cursor: not-allowed;
  transform: none !important;
  box-shadow: none !important;
  border-color: rgba(255, 255, 255, 0.18);
}

.motion-btn:disabled .motion-btn__bg {
  filter: grayscale(0.18) brightness(0.92);
  transform: none !important;
}

.motion-btn:disabled .motion-btn__row--top .motion-btn__char,
.motion-btn:disabled .motion-btn__row--bottom .motion-btn__char {
  transform: none;
}

.motion-btn:focus-visible {
  outline: 2px solid #e5aea9;
  outline-offset: 3px;
}

.motion-btn__sr {
  position: absolute;
  width: 1px;
  height: 1px;
  margin: -1px;
  padding: 0;
  overflow: hidden;
  border: 0;
  clip: rect(0 0 0 0);
  white-space: nowrap;
}

.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(1) { transition-delay: 0ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(2) { transition-delay: 16ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(3) { transition-delay: 24ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(4) { transition-delay: 32ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(5) { transition-delay: 40ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(6) { transition-delay: 48ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(7) { transition-delay: 56ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(8) { transition-delay: 64ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(9) { transition-delay: 72ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(10) { transition-delay: 80ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(11) { transition-delay: 88ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(12) { transition-delay: 96ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(13) { transition-delay: 104ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(14) { transition-delay: 112ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(15) { transition-delay: 120ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(16) { transition-delay: 128ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(17) { transition-delay: 136ms; }
.motion-btn:hover .motion-btn__row .motion-btn__char:nth-child(18) { transition-delay: 144ms; }

@media (prefers-reduced-motion: reduce) {
  .motion-btn {
    transition: none;
  }

  .motion-btn:hover {
    transform: none;
    box-shadow: none;
  }

  .motion-btn__bg,
  .motion-btn__char {
    transition: none;
    transform: none !important;
  }

  .motion-btn__row--bottom {
    display: none;
  }
}
</style>
