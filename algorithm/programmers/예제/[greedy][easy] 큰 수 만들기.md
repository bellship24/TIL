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

- [URL](https://programmers.co.kr/learn/courses/30/lessons/42883)

# Problem

![](/.uploads/2021-08-16-17-04-30.png)

# Approach

문제 요약

- 주어진 숫자 number 에서 k 개의 숫자를 제거했을 때 최대가 되도록 하라
- number : 문자열 형식의 숫자 (1 <= number <= 1,000,000)
- k : 제거할 숫자 개수 (1 <= k < number 의 자릿수)

문제 해석



예제

``` txt
number = 1231234, k = 3


```

로직

``` txt

```

로직 적용한 예제

``` txt

```

# First my code

``` py

```

# Refined my code

# Other's code

``` py
def solution(number, k):
    # stack 에 입력값을 순서대로 삽입
    stack = [number[0]]
    for num in number[1:]:
        # 들어오는 값이 stack 값보다 크면, 기존의 값을 제거하고 새로운 값으로 바꿈
        # 참고 : len(stack) > 0 == stack
        while len(stack) > 0 and stack[-1] < num and k > 0:
            # 값을 한 개 뺌
            k -= 1
            # 기존 stack 값 제거
            stack.pop()
        stack.append(num)
    # 만일 충분히 제거하지 못했으면 남은 부분은 단순하게 삭제
    # 이렇게 해도 되는 이유는 이미 큰 수부터 앞에서 채워넣었기 때문
    
    if k != 0:
        stack = stack[:-k]
    
    return ''.join(stack)
```

# Note