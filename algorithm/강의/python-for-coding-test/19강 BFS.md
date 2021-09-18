# BFS 란?

![](/.uploads2/2021-09-19-01-18-55.png)

- BFS 의 특징은 해당 노드의 인접 노드 중에 방문하지 않은 노드들을 모두 큐에 삽입하고 방문처리 한다는 것이다.

# BFS 동작 예시

![](/.uploads2/2021-09-19-01-21-24.png)
![](/.uploads2/2021-09-19-01-21-41.png)
![](/.uploads2/2021-09-19-01-22-01.png)
![](/.uploads2/2021-09-19-01-22-31.png)
![](/.uploads2/2021-09-19-01-23-07.png)
![](/.uploads2/2021-09-19-01-22-52.png)
![](/.uploads2/2021-09-19-01-23-43.png)

- BFS 의 이러한 특징으로 인해 각 간선의 비용이 모두 동일한 상황에서 최단거리의 문제를 해결하기 위해 사용될 수 있다는 점이 있다.

# BFS 소스코드 예제

![](/.uploads2/2021-09-19-01-25-34.png)

``` py
from collections import deque

# DFS 메서드 정의
def bfs(graph, start, visited):
  # Queue 구현을 위해 deque 라이브러리 사용
  queue = deque([start])
  # 현재 노드를 방문 처리
  visited[start] = True

  # 큐가 빌 때까지 방문 처리
  while queue:
    # 큐에서 하나의 원소를 뽑아 출력하기
    v = queue.popleft()
    print(v, end=' ')
    # 아직 방문하지 않은 인접한 원소들을 큐에 삽입
    for i in graph[v]:
      if not visited[i]:
        queue.append(i)
        visited[i] = True


# 각 노드가 연결된 정보를 표현 (2차원 리스트)
graph = [
  [],
  [2, 3, 8],
  [1, 7],
  [1, 4, 5],
  [3, 5],
  [3, 4],
  [7],
  [2, 6, 8],
  [1, 7]
]

# 각 노드가 방문된 정보를 표현 (1차원 리스트)
visited = [False] * 9

# 정의된 DFS 함수 호출
bfs(graph, 1, visited)

'''
1 2 3 8 7 4 5 6 
'''
```