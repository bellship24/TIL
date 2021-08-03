**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [Code](#code)
- [Answer](#answer)
- [Discussion](#discussion)

---

# Link

- [URL](https://www.hackerrank.com/challenges/the-birthday-bar/problem?isFullScreen=true&h_r=next-challenge&h_v=zen)

# Problem

![](/.uploads/2021-08-04-00-21-42.png)
![](/.uploads/2021-08-04-00-22-06.png)
![](/.uploads/2021-08-04-00-22-19.png)

# Approach

문제 해석

- 아래 두 조건에 따라 사각형에 정수가 있는 초콜렛을 얼마나 나눌 수 있는지?
  - 사각형의 length = Ron 의 birth month
  - 사각형에 있는 정수의 합 = Ron 의 birth day

- n : 초콜릿바에 있는 사각형의 개수 1 <= n <= 100
- s[i] : 초콜렛 사각형에 있는 정수 배열 i <= s[i] <= 5 where (0 <= i < n)
- d : Ron 의 birth day 1 <= d <= 31
- m : Ron 의 birth month 1 <= m <= 12

로직

``` txt
var cnt int32
var sum int32

for i, v := range s {
    sum += v
    if i == m - 1 {
        if sum == d {
            cnt++
        }

    }
}
```

예제

``` txt
// Input
5
1 2 1 3 2
3 2

2개 짜를 수 있음

1+2 = 3 (o)
2+1 = 3 (o)
1+3 = 4
3+2 = 5

return 2
```

# Code

``` go

```

# Answer

``` go
func birthday(s []int32, d int32, m int32) int32 {
    var cnt int32
    var sum int32
    for i, v := range s {
        sum += v
        if i >= int(m) - 1 {
            if sum == d {
                cnt++
            }
            sum -= s[i - int(m) + 1]
        }
    }
    return cnt
}
```

# Discussion

- 초콜릿을 나누는 거는 이어져 있는 초콜릿을 나누는 것이므로 앞,뒤를 한 칸씩 이동하면서 조건문을 점검한다고 보면 된다.