**목차**

- [1. TL;DR](#1-tldr)

**참고**

- [[Blog] Dynamic Programming 이란?](https://galid1.tistory.com/507)

---

# 1. TL;DR

- DP 는 큰 문제를 작은 문제들로 분할하여 풀이하고 메모리에 저장해놨다가 큰 문제를 해결할 때 사용하는 방법
- 분할정복과 다른 점은 동적 계획법의 경우, 작은 부분 문제의 답이 항상 같고 반복된다는 점

# 2. Dynamic Programming(DP) 이란?

- 동적 계획법
- 큰 문제를 반복되는 작은 문제로 나누어 푸는 방법

# 3. Dynamic Programming (동적 계획법) 과 Divide and Conquer (분할 정복) 의 차이점

- 공통점: 문제를 작은 단위로 분할하여 푸는 방법
- 차이점: DP 는 작은 문제들의 반복이 있고 DC 는 반복이 없음

# 4. DP 방법

- 모든 작은 문제는 한 번만 풀이
- 작은 문제에 대한 답을 적어놓고 그 보다 큰 문제를 풀 때 사용

# 5. DP 조건

- 작은 문제가 반복이 일어나는 경우
- 같은 문제는 구할 때마다 정답이 같음

# 6. Memoization 이란?

- 메모이제이션
- DP 에서 작은 문제들은 반복되고 결과값은 항상 같은데 이점을 이용해 한번 계산한 작은 문제의 답을 저장해놓고 사용하는 방법

# 7. DP 대표적인 예시 (피보나치 수열)

- 피보나치는 `1, 1, 2, 3, 5, 8, ...` 이라는 수열을 갖게 되고 즉, `다음 수열 = 이전 수열 + 두 단계 전 수열의 합` 이라는 점화식을 갖는다.
- 예를 들어, F(5) = F(4) + F(3) 이고 F(4) = F(3) + F(2) 이다. 이 경우, F(5) 에서도 F(3) 이 필요하고 F(4) 에서도 F(3) 이 필요하므로 작은 문제가 반복되는 구조이다.
- 또한, 피보나치 수열의 첫번째, 두번째 수열은 1 로 고정되어 있어 언제 계산해도 그 수열의 값들은 동일하다.

n 번째 피보나치 수열을 구하는 문제를 동적프로그래밍으로 푼 코드

``` python
def memoization_fibo(n):
    memo[0] = 1
    memo[1] = 1

    if n < 2:
        return memo[n]

    for i in range(2, n+1):
        memo[i] = memo[i-2] + memo[i-1]

    return memo[n]

if __name__ == '__main__':
    n = int(sys.stdin.readline())
    memo = [0 for i in range(n+2)]
    print(memoization_fibo(n))
```

# 8. Bottom-up 과 Top-down

## 8.1. Bottom-up

Bottom-up 은 작은 문제부터 차근차근 구해나가는 방법

``` python
def fibonacci_bottom_up(n):
    if n <= 1:
        return n

    fir = 0
    sec = 1
    for i in range(0, n-1):
        next = fir+sec
        fir = sec
        sec = next
    return next

if __name__ == '__main__':
    n = int(sys.stdin.readline())
    print(fibonacci_bottom_up(n))
```

## 8.2. Top-down

재귀함수로 구현하는 경우가 대부분 Top-down. 큰 문제를 풀 때, 작은 문제가 아직 풀리지 않았다면 그제서야 작은 문제를 해결하게 된다.

``` python
def fibonacci_top_down(n):
    if memo[n] > 0:
        return memo[n]

    if n <= 1:
        memo[n] = n
        return memo[n]

    else:
        memo[n] = fibonacci_top_down(n-1) + fibonacci_top_down(n-2)
        return memo[n]

if __name__ == '__main__':
    memo = [0 for i in range (100)]
    n = int(sys.stdin.readline())
    print(fibonacci_top_down(n))
```

# 9. DP 문제를 푸는 팁

- 어떤 큰 문제가 있을 때 그 문제의 가장 작은 문제부터 생각하고 `dp[0], dp[1], dp[2], dp[3]` 이렇게 작은 문제를 해결하다 보면 규칙을 발견하게 된다. `dp[4]` 를 해결할 때 즈음에는 이전에 구해놓은 작은 문제들을 이용해 점화식을 도출해낼 수 있다.