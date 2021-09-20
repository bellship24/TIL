# 예제 1 : 음료수 얼려먹기

## 문제 설명

![](/.uploads2/2021-09-19-02-57-48.png)
![](/.uploads2/2021-09-19-02-59-43.png)

- n,m 의 범위를 고려했을 때 전체 얼음 틀의 개수는 1백만개이하가 된다.

## 해결 방안

![](/.uploads2/2021-09-19-03-02-12.png)
![](/.uploads2/2021-09-19-03-03-30.png)

## 답안 소스 코드

![](/.uploads2/2021-09-19-03-04-29.png)

- 상하좌우에 대해서 `dfs()` 함수를 재귀적으로 호출하는 부분은 리턴값을 활용하지는 않고 `0` 으로 방문하지 않은 노드들에 대해 `1` 로 방문 처리 하기 위함이다.

``` py
# N, M 을 공백 기준으로 구분하여 입력 받기
n, m = map(int, input().split())

# 2 차원 리스트의 맵 정보 입력 받기
graph = []
for i in range(n):
  graph.append(list(map(int, input())))

# DFS 로 특정 노드를 방문하고 연결된 모든 노드들도 방문
def dfs(x, y):
  # 주어진 범위를 벗어나는 경우에는 즉시 종료
  if x <= -1 or x >= n or y <= -1 or y >= m:
    return False
  # 현재 노드를 아직 방문하지 않았다면
  if graph[x][y] == 0:
    # 해당 노드 방문 처리
    graph[x][y] = 1
    # 상, 하, 좌, 우의 위치들도 모두 재귀적으로 호출
    dfs(x - 1, y)
    dfs(x, y - 1)
    dfs(x + 1, y)
    dfs(x, y + 1)
    return True
  return False

# 모든 노드(위치)에 대하여 음료수 채우기
result = 0
for i in range(n):
  for j in range(m):
    # 현재 위치에서 DFS 수행
    if dfs(i, j) == True:
      result += 1

# 정답 출력
print(result)

'''
3
'''
```

# 예제 2 : 미로 탈출

## 문제 설명

![](/.uploads2/2021-09-20-01-49-40.png)
![](/.uploads2/2021-09-20-01-50-38.png)

## 해결 방안

![](/.uploads2/2021-09-20-01-55-17.png)

- BFS 는 간선의 비용이 모두 같을 때 최단거리를 탐색하기 위해 사용할 수 있는 알고리즘

![](/.uploads2/2021-09-20-01-56-37.png)
![](/.uploads2/2021-09-20-01-56-51.png)

- 여기서 노드를 증가시키면서 거리값을 증가시켜주면 된다.

![](/.uploads2/2021-09-20-01-57-59.png)

## 답안 소스 코드

![](/.uploads2/2021-09-20-02-01-37.png)

``` py
from collections import deque

# N, M 을 공백 기준으로 구분하여 입력 받기
n, m = map(int, input().split())

# 2 차원 리스트의 맵 정보 입력 받기
graph = []
for i in range(n):
  graph.append(list(map(int, input())))

# 이동할 네 가지 방향 정의 (상, 하, 좌, 우)
dx = [-1, 1, 0, 0]
dy = [0, 0, -1, 1]

# BFS 소스코드 구현
def bfs(x, y):
  # 큐(Queue) 구현을 위해 deque 라이브러리 사용
  queue = deque()
  queue.append((x, y))
  # 큐가 빌 때까지 반복하기
  while queue:
    x, y = queue.popleft()
    # 현재 위치에서 4 가지 방향으로의 위치 확인
    for i in range(4):
      nx = x + dx[i]
      ny = y + dy[i]
      # 미로 찾기 공간을 벗어난 경우 무시
      if nx < 0 or nx >= n or ny < 0 or ny >= m:
        continue
      # 벽인 경우 무시
      if graph[nx][ny] == 0:
        continue
      # 해당 노드를 처음 방문하는 경우에만 최단 거리 기록
      if graph[nx][ny] == 1:
        graph[nx][ny] = graph[x][y] + 1
        queue.append((nx, ny))
  # 가장 오른쪽 아래까지의 최단 거리 반환
  return graph[n - 1][m - 1]

print(bfs(0, 0))
```