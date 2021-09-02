# Link

- [URL](https://www.hackerrank.com/challenges/kangaroo/problem)

# Problem

![](/.uploads/2021-08-03-09-27-29.png)
![](/.uploads/2021-08-03-09-27-39.png)

# Approach

문제 해석

- 캥거루 두 마리가 각자 특정 지점, 특정 속도로 뛰어갈 때, 동일 지점에서 동시간에 만나게 된다면 true 아니면 false 반환

로직

``` txt
if v1 <= v2 {
    return false
} else {
    if v2 % v1 == 0 {
        return true
    } else {
        return false
    }
}
```

예제

``` txt
Input : 0 4 5 3

x1 < x2
v1 > v2

0 4 8  12 16 20
5 8 11 14 17 20

(x2 - x1) % (v1 - v2) -> 0


Output : false
```

# Code

``` go
func kangaroo(x1 int32, v1 int32, x2 int32, v2 int32) string {
    if v1 > v2 && (x2 - x1) % (v2 - v1) == 0 {
        return "YES"
    }
    return "NO"
}
```

# Answer

(동일)

# Discussion

- 시간복잡도 O(1) 인 답안
- 되도록 for 문을 안쓰는 방법을 고려하자
- 되도록 여러개 if 문을 and 연산으로 합칠 수 있는지 고려하자
- **두 개의 위치와 속도가 다를 때 만나는지에 대한 문제는 길이 차이를 속도 차이로 나눈 나머지가 0 인지를 보고 확인할 수 있다. → `(x2-x1)%(v1-v2) == 0` 즉, 두 개의 거리 차이를 두 개의 속도 차이로 나눴을 때 나머지가 0 이면 만난다.**