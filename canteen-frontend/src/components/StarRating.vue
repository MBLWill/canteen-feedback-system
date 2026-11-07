<template>
  <el-rate
    v-if="readonly"
    :model-value="value"
    disabled
    :max="5"
    :colors="['#ff7a1a', '#ff7a1a', '#ff7a1a']"
    disabled-void-color="#e0e3e8"
  />
  <el-rate
    v-else
    :model-value="value"
    :max="5"
    :colors="['#ff7a1a', '#ff7a1a', '#ff7a1a']"
    void-color="#e0e3e8"
    @update:model-value="val => emit('update:value', val)"
  />
</template>

<script setup>
defineProps({
  value: { type: Number, default: 0 },
  readonly: { type: Boolean, default: false }
})
const emit = defineEmits(['update:value'])
</script>
