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

- [URL](https://www.hackerrank.com/challenges/coin-change/problem)

# Problem

![](/.uploads/2021-08-05-20-58-26.png)
![](/.uploads/2021-08-05-20-58-43.png)

# Approach

문제 요약

- 사용가능한 동전의 양과 단위가 주어지면 얼마나 많은 잔돈을 만들 수 있는지 구해라.

문제 해석

예제

``` txt
Input
10 4
2 5 3 6

-> 주어진 4 종류 코인으로 10 원 맞추기

1. {2, 2, 2, 2, 2}
2. {2, 2, 3, 3}
```

로직

``` txt

```

로직 적용한 예제

``` txt

```

# First my code

``` go

```

# Refined my code

# Other's code

``` go
func getWays(n int32, coins []int64) int64 {
    var tabble [999][999]int        
    for i:=0;i<int(len(coins));i++{
        tabble[i][0] = 1
    }    
    for j:=1;j<=int(n);j++{
        if j % int(coins[0]) == 0{
            tabble[0][j] = 1
        }    else{
            tabble[0][j] = 0
        }        
    }

    for i:=1;i<int(len(coins));i++ { 
        for j:=1;j<=int(n);j++ {
            if int(coins[i]) > j  {
                tabble[i][j] = tabble[i-1][j]
            }else{                
                tabble[i][j] = tabble[i][j-int(coins[i])]  + tabble[i-1][j]
            }
        }
    }
    return int64(tabble[len(coins)-1][n])
```

# Note