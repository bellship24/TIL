**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [First my code](#first-my-code)
- [Refined my code](#refined-my-code)
- [Other's code](#others-code)
- [Note](#note)

---

# Link

- [URL](https://programmers.co.kr/learn/courses/30/lessons/42862?language=python3)

# Problem

![](/.uploads/2021-08-16-14-42-58.png)
![](/.uploads/2021-08-16-14-43-13.png)

# Approach

문제 요약

- 체육복이 있어야 체육 수업을 들을 수 있는데 수업을 들을 수 있는 학생의 최대값을 리턴하라
- 여벌 체육복을 가져온 학생만 다른 학생에게 빌려줄 수 있음
- 여벌 체육복을 가져온 학생이 체육복을 도난당했을 수 있고 이 때 하나만 도난당했다고 가정하며 남는 체육복이 하나 이므로 다른 학생에게 빌려줄 수 없음
- n : 전체 학생 수 (2 <= n <= 30)
- lost : 체육복 도난 당한 학생들의 번호가 담긴 배열 (1 <= len(lost) <= n, 중복x)
- reserve : 여벌의 체육복을 가져온 학생들의 번호가 담긴 배열 (1 <= len(reserve) <= n, 중복x)

문제 해석

- lost 배열에 대해 반복문을 통해 각 노단 당한 학생들이 reserve 배열로 부터 체육복을 빌릴 수 있는지 iteration 마다 해결 가능한지 검토하는 greedy 를 통해 문제를 해결하자

예제

``` txt
n=5, lost=[2, 4], reserve=[1, 3, 5], return=5

lost 에대해 반복문을 통해 reserve i 있는지 확인하고 여분이 있으나 도난된 경우, 남을 빌려줄 수 없고 본인이 써야하므로 lost 와 reserve 에서 i 제외
- reserve 에 2 가 없다.
- reserve 에 4 가 없다.

lost 에대해 반복문을 통해 reserve i-1, i+1 있는지 확인하고 빌려서 lost 와 reserve 에서 i 제외
- lost 2
  - reserve 에 `2` 있는지 확인.
    - 있다면, 다음 iteration
    - 없다면, 계속
  - reserve 에 `2-1` 이 있는지 확인.
    - 있다면,
      - reserve 1 에서 빌려옴
      - lost 에서 제외

- lost 4
  - reserve 3 에서 빌려옴
```

로직

``` txt
lost 반복문
    if reserve 에 i-1 있다면 
        lost[i] 와 resserve[i-1] 삭제
    else if reserve 에 i 있다면
        lost[i] 와 reserve[i] 삭제
    else if reserve 에 i+1 있다면
        lost[i] 와 reserve[i+1] 삭제

return n - len(lost)
```

로직 적용한 예제

``` txt
n = 5, lost = [1, 3, 4], reserve = [2, 3, 5]
1
  reserve 에 2 있어
    lost 에서 1 삭제
    reserve 에서 2 삭제
    즉, lost = [3, 4], reserve = [3, 5]
3
  reserve 에 3 있어
    lost 에서 3 삭제
    reserve 에서 3 삭제
    즉, lost = [4], reserve = [5]
4
  reserve 에 5 있어
    lost 에서 4 삭제
    reserve 에서 5 삭제
    즉, lost = [], reserve = []

return 5 - 0 = 5
```

# First my code

# Refined my code

# Other's code

``` go
def solution(n, lost, reserve):
    _reserve = [r for r in reserve if r not in lost]
    _lost = [l for l in lost if l not in reserve]
    for r in _reserve:
        f = r - 1
        b = r + 1
        if f in _lost:
            _lost.remove(f)
        elif b in _lost:
            _lost.remove(b)
    
    return n - len(_lost)
```

# Note