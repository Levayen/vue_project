<template>
  <div class="chess-piece" :class="color"></div>
</template>

<script>
export default {
  name: 'ChessPiece',
  props: {
    color: {
      type: String,
      required: true,
      validator: (value) => {
        return ['black', 'white'].includes(value);
      }
    }
  }
};
</script>

<style scoped>
.chess-piece {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  animation: placePiece 0.3s ease-in-out;
}

.chess-piece.black {
  background-color: #000000;
}

.chess-piece.white {
  background-color: #ffffff;
  border: 1px solid #000000;
}

@keyframes placePiece {
  from {
    transform: scale(0);
  }
  to {
    transform: scale(1);
  }
}
</style>