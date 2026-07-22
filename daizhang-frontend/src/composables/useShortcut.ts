import { onMounted, onUnmounted } from 'vue'

interface ShortcutOptions {
  ctrl?: boolean
  alt?: boolean
  shift?: boolean
  key: string
  handler: () => void
}

/**
 * 快捷键 composable(P5.3.1)
 * 在组件 onMounted 时注册,onUnmounted 时自动清理。
 * 避免在 input/textarea 中触发带 Ctrl 的组合键(允许 Ctrl+S 等系统级组合)。
 */
export function useShortcut(shortcuts: ShortcutOptions[]) {
  const handleKeydown = (e: KeyboardEvent) => {
    for (const sc of shortcuts) {
      if (sc.ctrl && !e.ctrlKey) continue
      if (sc.alt && !e.altKey) continue
      if (sc.shift && !e.shiftKey) continue
      if (e.key.toLowerCase() !== sc.key.toLowerCase()) continue

      // 纯字母快捷键(无 Ctrl/Alt)在输入框中不触发
      const target = e.target as HTMLElement
      const isInput = target.tagName === 'INPUT' || target.tagName === 'TEXTAREA' || target.isContentEditable
      if (isInput && !sc.ctrl && !sc.alt) continue

      e.preventDefault()
      sc.handler()
      return
    }
  }

  onMounted(() => window.addEventListener('keydown', handleKeydown))
  onUnmounted(() => window.removeEventListener('keydown', handleKeydown))
}
