/** 商品图占位（避免 via.placeholder 超时） */
export function productImage(image, id, size = 300) {
  if (image && !image.includes('via.placeholder.com')) {
    return image
  }
  const seed = id || 'default'
  return `https://picsum.photos/seed/${seed}/${size}/${size}`
}
