# Link

[URL](https://www.hackerrank.com/challenges/compare-the-triplets/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-02-01-45-29.png)
![](/.uploads/2021-08-02-01-45-43.png)

# Approach

샘플 풀이

``` txt
a = [5, 6, 7]
b = [3, 6, 10]

5 > 3 이므로 a +1
6 = 6 이므로 득점 없음
7 < 10 이므로 b +1

return [1, 1]
```

로직

``` txt
rst = [a 최종 점수, b 최종 점수]

각 요소 마다
a, b 크기 비교해서 a, b 점수 매기기
결과 출력
```

# Code

``` go
func compareTriplets(a []int32, b []int32) []int32 {
    rst := []int32{0, 0}
    for i := range a {
        if a[i] > b[i] {
            rst[0]++
        } else if a[i] < b[i] {
            rst[1]++
        }
    }
    return rst
}
```

# Answer

(동일)

# Discussion

- slice 의 값 초기화는 중괄호를 사용

``` go
rst := []int32{0, 0}
```