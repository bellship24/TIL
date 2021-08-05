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

- [URL](https://www.hackerrank.com/challenges/minimum-absolute-difference-in-an-array/problem)

# Problem

![](/.uploads/2021-08-05-23-15-58.png)
![](/.uploads/2021-08-05-23-16-26.png)

# Approach

문제 요약

- 주어진 array 에서 두 쌍을 골라 절대 편차를 구할 때 그 값이 가장 작은 값을 찾아라.

문제 해석

input

- n: arr size
- arr[i]: n 사이즈의 array

예제

``` txt

```

로직

``` txt
최소값 선언

arr 정렬

arr 반복문
    diff := i, i+1 간에 절대편차
    min 갱신

min 리턴
```

로직 적용한 예제

``` txt

```

# First my code

``` go
func minimumAbsoluteDifference(arr []int32) int32 {
    var min int32 = math.MaxInt32
    var absDiff int32
        
    sort.Slice(arr, func(i, j int) bool {
        return arr[i] < arr[j]
    })
    
    for i := range arr[:len(arr)-1] {
        
        if arr[i] < arr[i+1] {
            absDiff = -(arr[i] - arr[i+1])
        } else {
        absDiff = (arr[i] - arr[i+1])
        }
        
        if min > absDiff {
            min = absDiff
        }
    }
    return min
}
```

- 아래에 refined my code 처럼 abs 함수를 만들어 사용해도 되고 위 처럼 만들지 않고 진행해도 된다.

# Refined my code

``` go
func abs(x int32) int32 {
    if x < 0 {
        return -x
    }
    return x
}

func minimumAbsoluteDifference(arr []int32) int32 {
    var min int32 = math.MaxInt32
    
    sort.Slice(arr, func(i, j int) bool {
        return arr[i] < arr[j]
    })
    
    for i:=0; i<len(arr)-1; i++ {
        diff := abs(arr[i] - arr[i+1])
        if min > diff {
            min = diff
        }
    }
    return min
}
```

# Other's code

# Note

- 주어진 array 에서 두 개의 수를 비교하여 절대편차가 가장 작은 최소값을 구하는 문제인데 가장 작은 최소값이 계속 업데이트 되므로 greedy 알고리즘이라고 할 수 있다. 즉, 반복문의 iteration 에 따라 최소값이 업데이트 될 수 있다.
- 탐욕문제는 우선 정렬을 하자. 그 다음 배열에 대해 반복문을 돌리며 주어진 조건에 맞는 경우를 보고 값들을 갱신해나간다.
- **배열의 요소들 간에 크기 비교 시에 얼필 보면 반복문을 여러 번 써야할 것처럼 보인다. 하지만, 정렬을 하고나면, 반복문을 한 번만 사용하면 된다.**
- 정렬 방법 → `sort.Slice()`