import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const currentAccountSetId = ref<number | null>(null)
  const sidebarCollapsed = ref(false)

  function setCurrentAccountSet(id: number) {
    currentAccountSetId.value = id
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  return {
    currentAccountSetId,
    sidebarCollapsed,
    setCurrentAccountSet,
    toggleSidebar
  }
})
