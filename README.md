
# Proof Number Based Monte-Carlo Tree Search

Repository with the main source files for the **Proof Number Based Monte-Carlo Tree Search** publication.

Full version of the paper is available on [ArXiv](https://arxiv.org/abs/2303.09449).

Abstract:

*This paper proposes a new game search algorithm, PN-MCTS, that combines Monte-Carlo Tree Search (MCTS) and Proof-Number Search (PNS). These two algorithms have been successfully applied for decision making in a range of domains. We define three areas where the additional knowledge provided by the proof and disproof numbers gathered in MCTS trees might be used: final move selection, solving subtrees, and the UCT formula. We test all possible combinations on different time settings, playing against vanilla UCT MCTS on several games: Lines of Action (7×7 and 8×8), MiniShogi, Knightthrough, Awari, and Gomoku. Furthermore, we extend this new algorithm to properly address games with draws, like Awari, by adding an additional layer of PNS on top of the MCTS tree. The experiments show that PN-MCTS confidently outperforms MCTS in 5 out of 6 game domains (all except Gomoku), achieving win rates up to 96.2% for Lines of Action.*

Cite as:
```
@article{Kowalski2023ProofNumber,
  author = {Kowalski, J. and Doe, E. and Winands, M. H. M. and G\'{o}rski, D. and Soemers, D. J. N. J.},
  title = {{Proof Number Based Monte-Carlo Tree Search}},
  note = {arXiv preprint arXiv:2303.09449},
  year = {2023},
}

```
