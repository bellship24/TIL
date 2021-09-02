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

- [URL](https://programmers.co.kr/learn/courses/9877/lessons/55764?language=python3)

# Problem

![](/.uploads/2021-08-08-02-21-51.png)

# Approach

문제 요약

- participant 배열에 참가자 이름이 요소로 들어가며 동명이인으로 중복될 수 있다.
- completion 은 완주한 참가자의 이름이 요소로 들어간다. participant 중에 한 사람만이 완주를 하지 못해 len(participant) - 1 = len(completion) 이 된다.
- 이 한명이 누구인지 찾아라.

문제 해석

- 주어진 participant 배열과 completion 배열을 sort 해서 반복문으로 인덱스 하나씩 점검하며 원소가 일치하지 않을 때 리턴하는 방식으로 풀수도 있지만 sort 의 경우, 최소 시간 복잡도가 O(NlogN) 이므로 보통 O(N) 시간복잡도를 가지는 해시를 사용하자.
- 해시를 사용할 경우, key 를 participant 요소인 선수들의 이름으로 하고 value 를 각 선수 이름이 나온 횟수로 만들자. 그 후, completion 의 요소들에 대해 반복문 하여 생성한 딕셔너리에 해당 key 에 대한 value 를 -1 하자. 마지막으로 이 딕셔너리의 value 가 1 인 key 가 정답이다.

예제

``` txt

```

로직

``` txt
participant 반복문
    dict 의 key 에 이름, value 에 1 혹은 ++
completion 반복문
    dict 의 key 에 이름, value 에 -1
dict 에 value 가 1 인 key 를 리턴
```

로직 적용한 예제

``` txt

```

# First my code

``` python

```

# Refined my code

``` python
def solution(participant, completion):
    d = {}
    for x in participant:
        d[x] = d.get(x, 0) + 1
    for x in completion:
        d[x] -= 1
    dnf = [k for k, v in d.items() if v > 0]
    answer = dnf[0]
    return answer
```

# Other's code

``` python

```

# Note

Hash(Dictionary) 를 사용해야 하는 이유

- 스트링 익덱스 사용
- 해시테이블로 읽고 쓰기의 시간복잡도 n

정렬보다 해시를 사용해야 하는 이유

- 정렬은 최소 시간 복잡도가 O(NlogN) 으로 더 길다.

`dict.get(key[, default])` : 딕셔너리에서 특정 키의 값 찾기

```python
dic = {"A": 1, "B": 2}
print(dic.get("A"))
print(dic.get("C"))
print(dic.get("C", "Not Found ! "))

''' Output
1
None
Not Found ! 
'''
```

`dict.items()` : 딕셔너리의 key 와 value 를 한꺼번에 for문으로 반복

```python
dic = {"A": 1, "B": 2, "C": 3}
for k, v in dic.items():
    print("key={key}, value={value}".format(key=k, value=v))

''' Output
key=A, value=1
key=B, value=2
key=C, value=3
'''
```