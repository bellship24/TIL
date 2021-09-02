**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [Code](#code)
- [Answer](#answer)
- [Discussion](#discussion)

---

# Link

- [URL](https://www.hackerrank.com/challenges/breaking-best-and-worst-records/problem)

# Problem

![](/.uploads/2021-08-03-22-50-56.png)
![](/.uploads/2021-08-03-22-51-05.png)

# Approach

문제 해석

- n 번의 게임에서 min 점수, max 점수 각각의 신기록 갱신 횟수를 구해라

로직

``` txt
min, max := scores[0], scores[0]
var cnt [] int32

for i, v := range scores[1:] {
    if v < min {
        min = v
        cnt[1]++
    } else if v > max {
        max = v
        cnt[0]++
    }
}

return cnt
```

예제

``` txt
# input
9
10 5 20 20 4 5 2 25 1

min, max := 10, 10

v = 5 일 때,
5 < 10 (o)
    min = 5
    cnt[1]++

v = 20 일 때,
20 < 5 (x)
20 > 10 (o)
    max =20
    cnt[0]++

```

# Code

``` go
func breakingRecords(scores []int32) []int32 {
    min, max := scores[0], scores[0]
    cnt := []int32{0, 0}
    
    for _, v := range scores {
        if v < min {
            min = v
            cnt[1]++
        } else if v > max {
            max = v
            cnt[0]++
        }
    }
    return cnt
}
```

# Answer

(동일)

# Discussion

- 슬라이스 선언과 동시에 리터럴 할당

``` go
mySlice := []int{1, 2, 3}
```