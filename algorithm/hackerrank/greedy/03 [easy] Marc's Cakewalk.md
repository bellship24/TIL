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

- [URL](https://www.hackerrank.com/challenges/marcs-cakewalk/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-06-11-28-16.png)
![](/.uploads/2021-08-06-11-28-23.png)

# Approach

문제 요약

- 먹은 컵케이크에 따라 걸어야 하는 miles 를 가장 낮게하는 먹는 순서를 결정하고 최소 miles 를 구해라.
- j: 먹은 컵케이크 개수
- c: 해당 컵케이크 칼로리
- 최소 걸어야 하는 miles: 2^j * c
- calorie []int32: 각 컵케이크 당 칼로리 배열

문제 해석

- c(칼로리) 가 가장 높은 순으로 내림차순 정렬하고 주어진 miles 공식에 맞춰 j 에 대해 반복문으로 계산
- 즉, 칼로리가 높은 컵케이크를 가장 적게 먹게 돼, 걸어야 하는 miles 는 최소가 됨

예제

``` txt

```

로직

- sum 선언
- sortedCalorie = calorie 내림차순 정렬
- for j, c := len(sortedCalorie)
  - sum += math.pow(2, j)* c

로직 적용한 예제

- Input
4
7 4 9 6
- sortedCalorie = [9, 7, 6, 4]
- for j, c := range(sortedCalorie)
- 9
  - sum = 2^0 * 9
- 7
  - sum = 2^0 * 9 + 2^1 * 7
...

# First my code

``` go
func marcsCakewalk(calorie []int32) int64 {
    var sum int64
    
    sort.Slice(calorie, func(i, j int) bool { return calorie[i] > calorie[j] })
    
    for j, c := range(calorie) {
        fmt.Println(j, c)
        sum += int64(math.Pow(2, float64(j))) * int64(c)
    }
    
    return sum
}
```

# Refined my code

# Other's code

# Note