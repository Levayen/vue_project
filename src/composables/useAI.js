// AI逻辑组合式函数
export function useAI() {
  // 获取所有合法落子位置
  const getValidMoves = (board) => {
    const moves = [];
    const size = board.length;
    for (let i = 0; i < size; i++) {
      for (let j = 0; j < size; j++) {
        if (board[i][j] === 0) {
          moves.push({ row: i, col: j });
        }
      }
    }
    return moves;
  };

  // 随机选择一个合法位置（简单难度）
  const getRandomMove = (board) => {
    const validMoves = getValidMoves(board);
    if (validMoves.length === 0) return null;
    const randomIndex = Math.floor(Math.random() * validMoves.length);
    return validMoves[randomIndex];
  };

  // 评估位置的分数（中等难度）
  const evaluatePosition = (board, row, col, player) => {
    let score = 0;
    const opponent = player === 1 ? 2 : 1;
    const size = board.length;
    
    // 检查八个方向
    const directions = [
      [0, 1],   // 横向
      [1, 0],   // 纵向
      [1, 1],   // 左上-右下
      [1, -1]   // 右上-左下
    ];
    
    directions.forEach(([dx, dy]) => {
      let playerCount = 0;
      let opponentCount = 0;
      let emptyCount = 0;
      
      // 正方向
      for (let i = 1; i <= 4; i++) {
        const newRow = row + i * dx;
        const newCol = col + i * dy;
        if (newRow >= 0 && newRow < size && newCol >= 0 && newCol < size) {
          if (board[newRow][newCol] === player) {
            playerCount++;
          } else if (board[newRow][newCol] === opponent) {
            opponentCount++;
            break;
          } else {
            emptyCount++;
            break;
          }
        } else {
          break;
        }
      }
      
      // 反方向
      for (let i = 1; i <= 4; i++) {
        const newRow = row - i * dx;
        const newCol = col - i * dy;
        if (newRow >= 0 && newRow < size && newCol >= 0 && newCol < size) {
          if (board[newRow][newCol] === player) {
            playerCount++;
          } else if (board[newRow][newCol] === opponent) {
            opponentCount++;
            break;
          } else {
            emptyCount++;
            break;
          }
        } else {
          break;
        }
      }
      
      // 根据连续棋子数量计算分数
      if (playerCount === 4) {
        score += 10000; // 五子连珠
      } else if (playerCount === 3 && emptyCount >= 2) {
        score += 1000; // 活四
      } else if (playerCount === 3 && emptyCount === 1) {
        score += 100; // 冲四
      } else if (playerCount === 2 && emptyCount >= 2) {
        score += 50; // 活三
      } else if (playerCount === 2 && emptyCount === 1) {
        score += 10; // 冲三
      } else if (playerCount === 1 && emptyCount >= 2) {
        score += 5; // 活二
      }
      
      // 防守分数
      if (opponentCount === 4) {
        score += 9000; // 防止对方五子连珠
      } else if (opponentCount === 3 && emptyCount >= 2) {
        score += 900; // 防止对方活四
      } else if (opponentCount === 3 && emptyCount === 1) {
        score += 90; // 防止对方冲四
      } else if (opponentCount === 2 && emptyCount >= 2) {
        score += 40; // 防止对方活三
      }
    });
    
    return score;
  };

  // 评估当前局面（中等难度）
  const evaluateBoard = (board, player) => {
    let bestScore = -Infinity;
    let bestMove = null;
    const validMoves = getValidMoves(board);
    
    validMoves.forEach(move => {
      const score = evaluatePosition(board, move.row, move.col, player);
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
    });
    
    return bestMove;
  };

  // 极小极大算法（困难难度）
  const minimax = (board, depth, alpha, beta, maximizingPlayer, player) => {
    const opponent = player === 1 ? 2 : 1;
    const validMoves = getValidMoves(board);
    
    // 终止条件
    if (depth === 0 || validMoves.length === 0) {
      let score = 0;
      const size = board.length;
      for (let i = 0; i < size; i++) {
        for (let j = 0; j < size; j++) {
          if (board[i][j] === player) {
            score += evaluatePosition(board, i, j, player);
          } else if (board[i][j] === opponent) {
            score -= evaluatePosition(board, i, j, opponent);
          }
        }
      }
      return { score };
    }
    
    if (maximizingPlayer) {
      let maxScore = -Infinity;
      let bestMove = null;
      
      for (const move of validMoves) {
        // 模拟落子
        board[move.row][move.col] = player;
        const { score } = minimax(board, depth - 1, alpha, beta, false, player);
        // 撤销落子
        board[move.row][move.col] = 0;
        
        if (score > maxScore) {
          maxScore = score;
          bestMove = move;
        }
        
        alpha = Math.max(alpha, score);
        if (beta <= alpha) {
          break; // 剪枝
        }
      }
      
      return { score: maxScore, move: bestMove };
    } else {
      let minScore = Infinity;
      
      for (const move of validMoves) {
        // 模拟落子
        board[move.row][move.col] = opponent;
        const { score } = minimax(board, depth - 1, alpha, beta, true, player);
        // 撤销落子
        board[move.row][move.col] = 0;
        
        if (score < minScore) {
          minScore = score;
        }
        
        beta = Math.min(beta, score);
        if (beta <= alpha) {
          break; // 剪枝
        }
      }
      
      return { score: minScore };
    }
  };

  // 获取AI落子位置
  const getAIMove = (board, player, difficulty) => {
    switch (difficulty) {
      case 1: // 简单
        return getRandomMove(board);
      case 2: // 中等
        return evaluateBoard(board, player);
      case 3: // 困难
        {
          const { move } = minimax(board, 3, -Infinity, Infinity, true, player);
          return move || getRandomMove(board);
        }
      default:
        return getRandomMove(board);
    }
  };

  return {
    getAIMove
  };
}