<script setup>
import { productImage } from '../utils/productImage'

defineProps({
  product: { type: Object, required: true }
})
defineEmits(['click'])
</script>

<template>
  <article class="product-card" @click="$emit('click')">
    <div class="img-wrap">
      <img :src="productImage(product.image, product.id, 300)" :alt="product.name" loading="lazy" />
    </div>
    <div class="info">
      <h4>{{ product.name }}</h4>
      <div class="price">¥{{ product.price }}</div>
    </div>
  </article>
</template>
