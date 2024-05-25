
# Proof Number Based Monte-Carlo Tree Search

Repository with the main source files for the [**Proof Number Based Monte-Carlo Tree Search**](https://doi.org/10.1109/TG.2024.3403750) article (Jakub Kowalski, Elliot Doe, Mark H. M. Winands, Daniel Górski, Dennis J. N. J. Soemers; *IEEE Transactions on Games*, 2024)

An extended version of the paper is available on [ArXiv](https://arxiv.org/abs/2303.09449).

Abstract:

*This paper proposes a new game-search algorithm, PN-MCTS, which combines Monte-Carlo Tree Search (MCTS) and Proof-Number Search (PNS). These two algorithms have been successfully applied for decision making in a range of domains. We define three areas where the additional knowledge provided by the proof and disproof numbers gathered in MCTS trees might be used: final move selection, solving subtrees, and the UCB1 selection mechanism. We test all possible combinations on different time settings, playing against vanilla UCT on several games: Lines of Action (7×7 and 8×8), MiniShogi, Knightthrough, and Awari. Furthermore, we extend this new algorithm to properly address games with draws, like Awari, by adding an additional layer of PNS on top of the MCTS tree. The experiments show that PN-MCTS is able to outperform MCTS in all tested game domains, achieving win rates up to  96.2 for Lines of Action.*

Cite as:
```
@article{Kowalski2024ProofNumber,
  author = {Kowalski, J. and Doe, E. and Winands, M. H. M. and G\'{o}rski, D. and Soemers, D. J. N. J.},
  title = {{Proof Number Based Monte-Carlo Tree Search}},
  journal = {IEEE Transactions on Games},
  volume = {},
  number = {},
  pages = {1--10},
  year = {2024},
}

```
