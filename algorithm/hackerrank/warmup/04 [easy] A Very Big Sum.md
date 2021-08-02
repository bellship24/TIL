# Link

- [URL](https://www.hackerrank.com/challenges/a-very-big-sum/problem)

# Problem

![](/.uploads/2021-08-02-02-51-47.png)

# Approach

# Code

``` go
func aVeryBigSum(ar []int64) int64 {
    var sum int64 = 0
    for _, v := range ar {
        sum += v
    }
    return sum
}
```

# Answer

(동일)

# Discussion

- 32 비트 integer 자료형인 int32 의 범위는 (-2^31) ~ (2^31 - 1) 혹은 [-2147483648, 2147483647] 이다. 즉, 10^10 이면 충분히 이 범위를 넘는다. 그러므로 int64 자료형을 사용하면 된다.