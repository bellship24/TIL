**목차**

- [Link](#link)
- [Problem](#problem)
- [Approach](#approach)
- [Code](#code)
- [Answer](#answer)
- [Discussion](#discussion)

---

# Link

- [URL](https://www.hackerrank.com/challenges/migratory-birds/problem?isFullScreen=true)

# Problem

![](/.uploads/2021-08-04-01-52-59.png)
![](/.uploads/2021-08-04-01-53-12.png)
![](/.uploads/2021-08-04-01-53-24.png)

# Approach

문제 해석

- 주어진 배열의 각 요소들은 bird type id 를 나타낸다.
- 가장 많이 보이며 id 그리고 가장 작은 수의 id 를 찾아라.

로직

``` txt
arr 에 반복문
    각 arr 의 값에 대한 map 의 key 로 만들고 ++ 하기

value 가 가장 큰 type 찾기
중복일 시에 type 수가 작은 것을 return
```

예제

``` txt
n = 6
arr = [1, 4, 4, 4, 5, 3]

각 타입의 빈도수 정리
type 1 = 1 bird
type 2 = 0 bird
type 3 = 1 bird
type 4 = 3 bird
type 5 = 1 bird

bird 수가 가장 많은 type 은 4
```

# Code

``` go

```

# Answer

``` go
func migratoryBirds(arr []int32) int32 {
    b := map[int32]int32{}
    currentId := int32(math.MaxInt32)
    max := int32(0)
    
    for _, v := range arr {
        b[v]++
        if b[v] > max {
            currentId = v
            max = b[v]
        } else if b[v] == max && v < currentId {
            currentId = v
        }
    }
    
    return currentId
}
```

# Discussion

- map 선언 방법

``` go
myMap := map[int32]int32{}
```