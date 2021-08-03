**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [Code](#code)
- [Answer](#answer)
- [Discussion](#discussion)

---

# Link

- [URL](https://www.hackerrank.com/challenges/divisible-sum-pairs/problem)

# Problem

![](/.uploads/2021-08-04-01-32-57.png)
![](/.uploads/2021-08-04-01-33-10.png)

# Approach

문제 해석

- 정수 배열 ar 과 양의 정수 k 가 주어졌을 때, 아래 조건에 맞는 (i, j) 개수를 구하라
  - i < j
  - ar[i] + ar[j] 를 k 로 나눠 떨어짐

로직

``` txt
var cnt int32

for i, v1 := range ar {
    for _, v2 := range ar[i:] {
        if (v1 + v2) % k == 0 {
            cnt++
        }
    }
}

return cnt
```

예제

``` txt

```

# Code

``` go
func divisibleSumPairs(n int32, k int32, ar []int32) int32 {
    var cnt int32

    for i, v1 := range ar {
        for _, v2 := range ar[i+1:] {
            if (v1 + v2) % k == 0 {
                cnt++
            }
        }
    }

    return cnt
}
```

# Answer

# Discussion

- 주어진 배열에서 요소 두 개씩 전체 비교하는 문제인데, 반복문이 2 개로 간단히 해결할 수 있지만 이를 해결하는 적절한 해답을 찾지 못했다.